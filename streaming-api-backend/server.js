const app = require('./src/app');
const { PORT } = require('./src/config/env');
const { closeBrowser }   = require('./src/ingestion/scraper/browserExtractor');

app.listen(PORT, () => {
  console.log(`API running on port ${PORT}`);
});

// Cierra el navegador al apagar el servidor
process.on('SIGTERM', async () => {
  await closeBrowser();
  process.exit(0);
});

process.on('SIGINT', async () => {
  await closeBrowser();
  process.exit(0);
});
