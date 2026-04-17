const router = require('express').Router();
const { reviews, tracks, users, uuidv4 } = require('../data/store');

router.get('/', (req, res) => {
  const { trackId, userId } = req.query;
  let result = reviews;
  if (trackId) result = result.filter(r => r.trackId === trackId);
  if (userId)  result = result.filter(r => r.userId  === userId);
  result = result.map(r => ({ ...r, author: users.find(u => u.id === r.userId)?.username ?? 'Unknown' }));
  res.json(result);
});

router.post('/', (req, res) => {
  const { trackId, userId, rating, text } = req.body;
  if (!trackId || !userId || !rating) return res.status(400).json({ error: 'trackId, userId, rating required' });
  if (!tracks.find(t => t.id === trackId)) return res.status(404).json({ error: 'Track not found' });
  if (!users.find(u => u.id === userId))   return res.status(404).json({ error: 'User not found' });
  const existing = reviews.find(r => r.trackId === trackId && r.userId === userId);
  if (existing) {
    existing.rating = Number(rating);
    existing.text   = text || '';
    return res.json(existing);
  }
  const review = { id: uuidv4(), trackId, userId, rating: Number(rating), text: text || '' };
  reviews.push(review);
  res.status(201).json(review);
});

router.delete('/:id', (req, res) => {
  const idx = reviews.findIndex(r => r.id === req.params.id);
  if (idx === -1) return res.status(404).json({ error: 'Not found' });
  reviews.splice(idx, 1);
  res.json({ ok: true });
});

module.exports = router;
