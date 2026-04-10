const ProviderB           = require('./providers/providerB');
const { normalizeStream } = require('../normalizer/streamNormalizer');

const providerB = new ProviderB();

// Solo ProviderB en el job de ingesta
// ProviderA se llama on-demand desde streams.service.js
const scrapeContent = async ({ tmdbId, contentId, contentType, title, year, season, episode }) => {
  const results = [];

  try {
    console.log(`[Scraper] Caching VidSrc embed for "${title}" (tmdb:${tmdbId})`);

    const rawStreams = await providerB.fetchStreams(
      tmdbId, contentType, title, year, season, episode
    );

    const normalized = rawStreams.map(raw =>
      normalizeStream({
        contentType,
        contentId,
        url:         null,
        embedUrl:    raw.embedUrl   || raw.embed_url,
        streamType:  'embed',
        qualityHint: raw.quality    || 'auto',
        serverName:  raw.serverName || raw.server_name,
        language:    raw.language   || 'en-sub',
      })
    );

    results.push(...normalized);
    console.log(`[Scraper] VidSrc — ${normalized.length} embed(s) cached for "${title}"`);

  } catch (err) {
    console.error(`[Scraper] Failed for "${title}":`, err.message);
  }

  return results;
};

module.exports = { scrapeContent };