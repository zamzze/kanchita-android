const { connect } = require('puppeteer-real-browser');

const extractM3u8WithBrowser = (url, referer = 'https://repelis24.sbs') => {
  return new Promise(async (resolve) => {
    const streamUrls = new Set();
    let browser      = null;
    let page         = null;
    let resolved     = false;

    const finish = async (result) => {
      if (resolved) return;
      resolved = true;
      clearTimeout(hardTimeout);
      try {
        if (page)    await page.close();
        if (browser) await browser.close();
      } catch {}
      resolve(result);
    };

    const hardTimeout = setTimeout(() => {
      console.log(`[Browser] Hard timeout for ${url} — found ${streamUrls.size} streams`);
      finish([...streamUrls]);
    }, 20000);

    try {
      const result = await connect({
        headless:       true,
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-web-security',
          '--disable-dev-shm-usage',
          '--disable-features=IsolateOrigins,site-per-process',
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
        'Accept-Language': 'es-ES,es;q=0.9,en;q=0.8',
        'Referer':         referer,
      });

      await page.evaluateOnNewDocument(() => {
        window.open = () => null;
      });

      page.on('dialog', async (d) => d.accept().catch(() => {}));

      await page.setRequestInterception(true);

      page.on('request', async (req) => {
        const reqUrl  = req.url();
        const resType = req.resourceType();

        // Bloquea ads y tracking
        const blocked = ['analytics', 'ads', 'disable-devtool', 'cloudflareinsights', 'histats', 'pixel.embed'];
        if (blocked.some(b => reqUrl.includes(b))) {
          await req.abort();
          return;
        }

        // Captura m3u8 — resuelve inmediatamente
        if (reqUrl.includes('.m3u8')) {
          streamUrls.add(reqUrl);
          console.log(`[Browser] m3u8 intercepted: ${reqUrl}`);
          await finish([...streamUrls]);
          return;
        }

        if (['image', 'stylesheet', 'font'].includes(resType)) {
          await req.abort();
        } else {
          await req.continue();
        }
      });

      await page.goto(url, {
        waitUntil: 'domcontentloaded',
        timeout:   18000,
      });

    } catch (err) {
      console.warn(`[Browser] Error for ${url}:`, err.message);
      await finish([...streamUrls]);
    }
  });
};

module.exports = { extractM3u8WithBrowser };