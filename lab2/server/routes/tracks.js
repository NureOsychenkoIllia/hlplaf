const router = require('express').Router();
const { tracks } = require('../data/store');
const { trackEvent } = require('../middleware/analytics');

router.get('/', (req, res) => {
  const { genre, q } = req.query;
  let result = tracks;
  if (genre) result = result.filter(t => t.genre.toLowerCase() === genre.toLowerCase());
  if (q)     result = result.filter(t =>
    t.title.toLowerCase().includes(q.toLowerCase()) ||
    t.artist.toLowerCase().includes(q.toLowerCase())
  );
  res.json(result);
});

router.get('/genres', (req, res) => {
  res.json([...new Set(tracks.map(t => t.genre))]);
});

router.get('/:id', (req, res) => {
  const track = tracks.find(t => t.id === req.params.id);
  if (!track) return res.status(404).json({ error: 'Not found' });
  res.json(track);
});

router.post('/:id/play', trackEvent('play'), (req, res) => {
  res.json({ ok: true });
});

router.post('/:id/download', trackEvent('download'), (req, res) => {
  const track = tracks.find(t => t.id === req.params.id);
  res.json({ ok: true, track });
});

module.exports = router;
