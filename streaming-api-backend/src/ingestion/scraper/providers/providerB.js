const BaseProvider = require('../baseProvider');

const VIDSRC_BASE = 'https://vsembed.ru';

class ProviderB extends BaseProvider {
  constructor() {
    super('ProviderB');
  }

  async fetchStreams(tmdbId, contentType, title, year, season, episode) {
    const streams = [];

    if (contentType === 'movie') {
      streams.push({
        url:        null,
        embedUrl:   `${VIDSRC_BASE}/embed/movie?tmdb=${tmdbId}&ds_lang=es&autoplay=1`,
        streamType: 'embed',
        serverName: 'VidSrc',
        language:   'en-sub',
        quality:    'auto',
      });
    } else {
      if (!season || !episode) return [];
      streams.push({
        url:        null,
        embedUrl:   `${VIDSRC_BASE}/embed/tv?tmdb=${tmdbId}&season=${season}&episode=${episode}&ds_lang=es&autoplay=1`,
        streamType: 'embed',
        serverName: 'VidSrc',
        language:   'en-sub',
        quality:    'auto',
      });
    }

    console.log(`[${this.name}] Built embed URL for tmdb:${tmdbId}`);
    return streams;
  }
}

module.exports = ProviderB;