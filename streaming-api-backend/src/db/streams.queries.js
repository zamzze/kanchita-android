const pool = require('../config/db');

const findStreams = async (contentType, contentId) => {
  const { rows } = await pool.query(
    `SELECT id, server_name, quality, language, stream_url, priority
     FROM streams
     WHERE content_type = $1 AND content_id = $2 AND is_active = TRUE
     ORDER BY priority ASC`,
    [contentType, contentId]
  );
  return rows;
};

const findEpisodeWithSeries = async (episodeId) => {
  const { rows } = await pool.query(
    `SELECT e.id, e.series_id, e.is_published, s.is_published AS series_published
     FROM episodes e
     JOIN series s ON s.id = e.series_id
     WHERE e.id = $1`,
    [episodeId]
  );
  return rows[0] || null;
};

module.exports = { findStreams, findEpisodeWithSeries };