const pool                      = require('../../config/db');
const moviesDb                  = require('../../db/movies.queries');
const { getActiveSubscription } = require('../../db/auth.queries');
const { replaceStreams }         = require('../../db/ingestion.queries');
const ProviderA                 = require('../../ingestion/scraper/providers/providerA');
const ProviderB                 = require('../../ingestion/scraper/providers/providerB');

const providerA = new ProviderA();
const providerB = new ProviderB();

// ─── Helpers ────────────────────────────────────────────────────────────────

const normalizeProviderStream = (s) => ({
  server_name: s.serverName || s.server_name,
  quality:     s.quality    || 'auto',
  language:    s.language   || 'es',
  stream_url:  null,
  embed_url:   s.embedUrl   || s.embed_url,
  stream_type: 'embed',
});

const scrapeProviderA = async (tmdbId, contentType, title, originalTitle, year, season, episode) => {
  try {
    let raw = await providerA.fetchStreams(tmdbId, contentType, title, year, season, episode);

    if (!raw.length && originalTitle && originalTitle !== title) {
      console.log(`[ProviderA] Retrying with original title: "${originalTitle}"`);
      raw = await providerA.fetchStreams(tmdbId, contentType, originalTitle, year, season, episode);
    }

    return raw.map(normalizeProviderStream);
  } catch (err) {
    console.warn('[Streams] ProviderA failed:', err.message);
    return [];
  }
};

const scrapeAndCacheProviderB = async (tmdbId, contentType, contentId, title, year, season, episode) => {
  try {
    const raw = await providerB.fetchStreams(tmdbId, contentType, title, year, season, episode);
    if (!raw.length) return [];

    const normalized = raw.map(s => ({
      content_type: contentType,
      content_id:   contentId,
      server_name:  s.serverName || s.server_name,
      quality:      s.quality    || 'auto',
      language:     s.language   || 'en-sub',
      stream_url:   null,
      embed_url:    s.embedUrl   || s.embed_url,
      stream_type:  'embed',
    }));

    await replaceStreams(contentType, contentId, normalized);
    return normalized.map(normalizeProviderStream);
  } catch (err) {
    console.warn('[Streams] ProviderB failed:', err.message);
    return [];
  }
};

// Solo trae streams de VidSrc (ProviderB) del caché
const getCachedProviderB = async (contentType, contentId) => {
  const { rows } = await pool.query(
    `SELECT server_name, quality, language,
            stream_url, embed_url, stream_type, priority
     FROM streams
     WHERE content_type = $1
       AND content_id   = $2
       AND server_name  = 'VidSrc'
       AND is_active    = TRUE
     ORDER BY priority ASC`,
    [contentType, contentId]
  );
  return rows;
};

const cacheAllStreams = async (contentType, contentId, streams) => {
  if (!streams.length) return;
  const toSave = streams.map(s => ({
    content_type: contentType,
    content_id:   contentId,
    server_name:  s.server_name,
    quality:      s.quality,
    language:     s.language,
    stream_url:   null,
    embed_url:    s.embed_url,
    stream_type:  'embed',
  }));
  await replaceStreams(contentType, contentId, toSave);
};

const formatResponse = (streams, contentId, contentType, subscription) => ({
  content_id:   contentId,
  content_type: contentType,
  show_ads:     !subscription,
  streams:      streams.map((s, i) => ({
    server_name: s.server_name,
    quality:     s.quality,
    language:    s.language,
    stream_url:  null,
    embed_url:   s.embed_url,
    stream_type: 'embed',
    priority:    i + 1,
  })),
});

// ─── Movie streams ───────────────────────────────────────────────────────────

const getMovieStreams = async (movieId, userId) => {
  const movie = await moviesDb.findById(movieId);
  if (!movie) {
    const err = new Error('Movie not found');
    err.statusCode = 404;
    throw err;
  }

  console.log(`[Streams] Fetching streams for "${movie.title}" (tmdb:${movie.tmdb_id})`);

  const cachedB = await getCachedProviderB('movie', movieId);

  // ProviderA siempre fresco + ProviderB de caché o scrapeado
  const [providerAStreams, providerBStreams] = await Promise.all([
    scrapeProviderA(
      movie.tmdb_id, 'movie',
      movie.title, movie.original_title,
      movie.release_year
    ),
    cachedB.length
      ? Promise.resolve(cachedB)
      : scrapeAndCacheProviderB(
          movie.tmdb_id, 'movie', movieId,
          movie.title, movie.release_year
        ),
  ]);

  // Latino primero, inglés como respaldo
  const allStreams = [...providerAStreams, ...providerBStreams];

  if (!allStreams.length) {
    const err = new Error('No streams available for this content');
    err.statusCode = 404;
    throw err;
  }

  await cacheAllStreams('movie', movieId, allStreams);

  const subscription = await getActiveSubscription(userId);
  return formatResponse(allStreams, movieId, 'movie', subscription);
};

// ─── Episode streams ─────────────────────────────────────────────────────────

const getEpisodeStreams = async (episodeId, userId) => {
  const { rows } = await pool.query(
    `SELECT e.id, e.series_id, e.season_number, e.episode_number,
            e.title, e.is_published, s.is_published AS series_published,
            s.tmdb_id, s.title AS series_title,
            s.original_title AS series_original_title,
            s.release_year
     FROM episodes e
     JOIN series s ON s.id = e.series_id
     WHERE e.id = $1`,
    [episodeId]
  );

  const episode = rows[0];
  if (!episode || !episode.is_published || !episode.series_published) {
    const err = new Error('Episode not found');
    err.statusCode = 404;
    throw err;
  }

  console.log(`[Streams] Fetching S${episode.season_number}E${episode.episode_number} of "${episode.series_title}"`);

  const cachedB = await getCachedProviderB('episode', episodeId);

  // ProviderA siempre fresco — busca latino en Cinecalidad
  // ProviderB de caché si ya existe, sino scrapea VidSrc
  const [providerAStreams, providerBStreams] = await Promise.all([
    scrapeProviderA(
      episode.tmdb_id, 'episode',
      episode.series_title, episode.series_original_title,
      episode.release_year,
      episode.season_number, episode.episode_number
    ),
    cachedB.length
      ? Promise.resolve(cachedB)
      : scrapeAndCacheProviderB(
          episode.tmdb_id, 'episode', episodeId,
          episode.series_title, episode.release_year,
          episode.season_number, episode.episode_number
        ),
  ]);

  const allStreams = [...providerAStreams, ...providerBStreams];

  if (!allStreams.length) {
    const err = new Error('No streams available for this content');
    err.statusCode = 404;
    throw err;
  }

  await cacheAllStreams('episode', episodeId, allStreams);

  const subscription = await getActiveSubscription(userId);
  return formatResponse(allStreams, episodeId, 'episode', subscription);
};

module.exports = { getMovieStreams, getEpisodeStreams };