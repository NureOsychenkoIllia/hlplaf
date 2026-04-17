const router = require('express').Router();
const { playlists, tracks, uuidv4 } = require('../data/store');

const withTracks = pl => ({ ...pl, tracks: pl.trackIds.map(id => tracks.find(t => t.id === id)).filter(Boolean) });

router.get('/', (req, res) => {
  const { userId } = req.query;
  let result = userId ? playlists.filter(p => p.userId === userId) : playlists;
  res.json(result.map(withTracks));
});

router.get('/:id', (req, res) => {
  const pl = playlists.find(p => p.id === req.params.id);
  if (!pl) return res.status(404).json({ error: 'Not found' });
  res.json(withTracks(pl));
});

router.post('/', (req, res) => {
  const { userId, name } = req.body;
  if (!userId || !name) return res.status(400).json({ error: 'userId and name required' });
  const pl = { id: uuidv4(), userId, name, trackIds: [] };
  playlists.push(pl);
  res.status(201).json(pl);
});

router.post('/:id/tracks', (req, res) => {
  const pl = playlists.find(p => p.id === req.params.id);
  if (!pl) return res.status(404).json({ error: 'Not found' });
  const { trackId } = req.body;
  if (!tracks.find(t => t.id === trackId)) return res.status(404).json({ error: 'Track not found' });
  if (!pl.trackIds.includes(trackId)) pl.trackIds.push(trackId);
  res.json(pl);
});

router.delete('/:id/tracks/:trackId', (req, res) => {
  const pl = playlists.find(p => p.id === req.params.id);
  if (!pl) return res.status(404).json({ error: 'Not found' });
  pl.trackIds = pl.trackIds.filter(id => id !== req.params.trackId);
  res.json(pl);
});

router.delete('/:id', (req, res) => {
  const idx = playlists.findIndex(p => p.id === req.params.id);
  if (idx === -1) return res.status(404).json({ error: 'Not found' });
  playlists.splice(idx, 1);
  res.json({ ok: true });
});

module.exports = router;
