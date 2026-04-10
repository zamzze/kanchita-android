// src/ingestion/scraper/providers/providerC.js
const { connect } = require('puppeteer-real-browser');

const CINEBY_BASE = 'https://cinebytv.com';

async function getStreamFromCineby(tmdbId, type, season = null, episode = null) {
    let browser, page;

    try {
        const url = type === 'movie'
            ? `${CINEBY_BASE}/movie/${tmdbId}`
            : `${CINEBY_BASE}/tv/${tmdbId}?season=${season}&episode=${episode}`;

        ({ browser, page } = await connect({
            headless: true,
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-web-security',
                '--disable-dev-shm-usage',
            ],
            turnstile: true,
            disableXvfb: false,
        }));

        await page.setUserAgent(
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36'
        );

        // Bloquear ads y tracking desde el inicio
        await page.evaluateOnNewDocument(() => {
            window.open = () => null;
        });
        page.on('dialog', async dialog => await dialog.accept());

        // Habilitar interceptación de requests
        await page.setRequestInterception(true);

        let m3u8Url = null;

        page.on('request', async request => {
            const reqUrl = request.url();

            // Bloquear tracking y ads
            const blocked = [
                'analytics', 'cloudflareinsights', 'histats',
                'pixel.embed', 'disable-devtool', 'umami'
            ];
            if (blocked.some(b => reqUrl.includes(b))) {
                await request.abort();
                return;
            }

            // Capturar el m3u8 de wind.10018.workers.dev
            if (reqUrl.includes('wind.10018.workers.dev') && reqUrl.includes('.m3u8')) {
                m3u8Url = reqUrl;
                console.log(`[ProviderC] M3U8 encontrado: ${reqUrl}`);
                await request.continue();
                // Cerrar la página apenas tenemos el m3u8
                setTimeout(() => page.close().catch(() => {}), 100);
                return;
            }

            await request.continue();
        });

        // Navegar a la página
        console.log(`[ProviderC] Navegando a: ${url}`);
        await page.goto(url, {
            waitUntil: 'domcontentloaded',
            timeout: 30000
        });

        // Esperar hasta que aparezca el m3u8 o timeout
        await Promise.race([
            new Promise(resolve => {
                const interval = setInterval(() => {
                    if (m3u8Url) {
                        clearInterval(interval);
                        resolve();
                    }
                }, 300);
            }),
            new Promise((_, reject) =>
                setTimeout(() => reject(new Error('Timeout: no m3u8 en 30s')), 30000)
            )
        ]);

        return m3u8Url;

    } catch (err) {
        console.error(`[ProviderC] Error: ${err.message}`);
        return null;
    } finally {
        if (browser) await browser.close().catch(() => {});
    }
}

module.exports = { getStreamFromCineby };