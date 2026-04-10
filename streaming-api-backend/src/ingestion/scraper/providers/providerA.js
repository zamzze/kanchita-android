const BaseProvider   = require('../baseProvider');

const PROVIDER_A_URL = process.env.PROVIDER_A_URL;

class ProviderA extends BaseProvider {
  constructor() {
    super('ProviderA');
  }

  async fetchStreams(tmdbId, contentType, title, year, season, episode) {
    if (!PROVIDER_A_URL) return [];

    let browser = null;
    let page    = null;

    try {
      const { connect } = require('puppeteer-real-browser');
      const result = await connect({
        headless:       true,
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-web-security',
          '--disable-dev-shm-usage',
        ],
        turnstile:      true,
        disableXvfb:    false,
        ignoreAllFlags: false,
      });

      browser = result.browser;
      page    = result.page;

      await page.setUserAgent(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36'
      );
      await page.setExtraHTTPHeaders({
        'Accept-Language': 'es-ES,es;q=0.9',
        'Referer':         PROVIDER_A_URL,
      });

      // Paso 1 — buscar la película
      const detailUrl = await this._search(page, title, year, contentType, season, episode);

      if (!detailUrl) return [];
      console.log(`[${this.name}] Found: ${detailUrl}`);

      // Paso 2 — extraer embed de Goodstream
      const embedUrl = await this._getGoodstreamEmbed(page, detailUrl);
      if (!embedUrl) return [];
      console.log(`[${this.name}] Goodstream embed: ${embedUrl}`);

      // Devolver como embed — Android TV lo resuelve con WebView
      return [{
        url:        null,
        embedUrl,
        streamType: 'embed',
        serverName: 'Goodstream Latino',
        language:   'es-lat',
        quality:    'auto',
      }];

    } catch (err) {
      console.error(`[${this.name}] Error for "${title}":`, err.message);
      return [];
    } finally {
      if (page)    await page.close().catch(() => {});
      if (browser) await browser.close().catch(() => {});
    }
  }

  async _search(page, title, year, contentType, season, episode) {
  await page.setRequestInterception(true);
  page.on('request', async (req) => {
    if (['image', 'stylesheet', 'font'].includes(req.resourceType())) {
      await req.abort();
    } else {
      await req.continue();
    }
  });

  const searchUrl = `${PROVIDER_A_URL}/?do=search&subaction=search&story=${encodeURIComponent(title)}`;
  await page.goto(searchUrl, { waitUntil: 'domcontentloaded', timeout: 15000 }).catch(() => {});

  const normalize = (str) =>
    str.toLowerCase()
       .normalize('NFD')
       .replace(/[\u0300-\u036f]/g, '')
       .replace(/[^a-z0-9\s]/g, '')
       .trim();

  const normalizedTitle = normalize(title);

  const result = await page.evaluate(() => {
    const links = [];
    document.querySelectorAll('a[href]').forEach(a => {
      const href = a.href;
      const text = a.textContent.trim();
      if (href.match(/cinecalidadhd\.world\/\d+-/) && text.length > 3) {
        links.push({ href, text });
      }
    });
    return links;
  });

  if (!result.length) return null;

  const exact = result.find(r => normalize(r.text) === normalizedTitle);
  const match = exact || result.find(r => normalize(r.text).includes(normalizedTitle)) || result[0];

  if (!match) return null;

  // Para episodios, agregar /temporada-X-episodio-Y al final
  if (contentType === 'episode' && season && episode) {
    const baseUrl = match.href.replace(/\/$/, '');
    return `${baseUrl}/temporada-${season}-episodio-${episode}`;
  }

  return match.href;
}

async _getGoodstreamEmbed(page, detailUrl) {
  await page.goto(detailUrl, { waitUntil: 'domcontentloaded', timeout: 15000 }).catch(() => {});

  const embed = await page.evaluate(() => {
    // Buscar en data-src (películas)
    const gsEl = document.querySelector('[data-src*="gscdn"]');
    if (gsEl) {
      const src = gsEl.getAttribute('data-src');
      return src.startsWith('//') ? 'https:' + src : src;
    }

    // Buscar en iframes directos (episodios)
    const iframes = document.querySelectorAll('iframe');
    for (const iframe of iframes) {
      const src = iframe.src || iframe.getAttribute('src');
      if (src && src.includes('gscdn')) {
        return src.startsWith('//') ? 'https:' + src : src;
      }
    }

    // Buscar en links
    const gsLink = document.querySelector('a[href*="gscdn.cam"]');
    if (gsLink) return gsLink.href;

    return null;
  });

  return embed;
}
}

module.exports = ProviderA;