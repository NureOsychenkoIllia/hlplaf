const router = require('express').Router();
const { users, playlists, reviews, analyticsEvents, uuidv4 } = require('../data/store');

const safe = user => { const { password: _, ...rest } = user; return rest; };

router.post('/login', (req, res) => {
  const { username, password } = req.body;
  const user = users.find(u => u.username === username && u.password === password);
  if (!user) return res.status(401).json({ error: 'Invalid credentials' });
  res.json(safe(user));
});

router.post('/register', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) return res.status(400).json({ error: 'username and password required' });
  if (users.find(u => u.username === username)) return res.status(409).json({ error: 'Username taken' });
  const user = { id: uuidv4(), username, password, avatar: username[0].toUpperCase(), playlists: [] };
  users.push(user);
  res.status(201).json(safe(user));
});

router.get('/:id/profile', (req, res) => {
  const user = users.find(u => u.id === req.params.id);
  if (!user) return res.status(404).json({ error: 'Not found' });
  res.json({
    ...safe(user),
    playlistCount: playlists.filter(p => p.userId === user.id).length,
    reviewCount:   reviews.filter(r => r.userId === user.id).length,
    totalPlays:    analyticsEvents.filter(e => e.userId === user.id && e.event === 'play').length,
  });
});

router.patch('/:id/profile', (req, res) => {
  const user = users.find(u => u.id === req.params.id);
  if (!user) return res.status(404).json({ error: 'Not found' });
  const { username, avatar } = req.body;
  if (username) {
    if (users.find(u => u.username === username && u.id !== user.id))
      return res.status(409).json({ error: 'Username taken' });
    user.username = username;
  }
  if (avatar) user.avatar = avatar;
  res.json(safe(user));
});

module.exports = router;
