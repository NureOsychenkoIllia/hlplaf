const router = require('express').Router();
const { tracks, analyticsEvents, uuidv4 } = require('../data/store');

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

router.post('/:id/play', (req, res) => {
  const track = tracks.find(t => t.id === req.params.id);
  if (!track) return res.status(404).json({ error: 'Not found' });
  analyticsEvents.push({
    id: uuidv4(), trackId: req.params.id,
    userId: req.headers['x-user-id'] || null,
    event: 'play', ts: new Date().toISOString(),
  });
  res.json({ ok: true });
});

router.post('/:id/download', (req, res) => {
  const track = tracks.find(t => t.id === req.params.id);
  if (!track) return res.status(404).json({ error: 'Not found' });
  analyticsEvents.push({
    id: uuidv4(), trackId: req.params.id,
    userId: req.headers['x-user-id'] || null,
    event: 'download', ts: new Date().toISOString(),
  });
  res.json({ ok: true, track });
});

module.exports = router;
