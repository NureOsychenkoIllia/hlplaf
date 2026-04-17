const router = require('express').Router();
const { analyticsEvents, tracks } = require('../data/store');

router.get('/summary', (req, res) => {
  const plays     = analyticsEvents.filter(e => e.event === 'play');
  const downloads = analyticsEvents.filter(e => e.event === 'download');

  const playsByTrack = {};
  plays.forEach(e => { playsByTrack[e.trackId] = (playsByTrack[e.trackId] || 0) + 1; });

  const topTracks = Object.entries(playsByTrack)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([trackId, count]) => {
      const t = tracks.find(tr => tr.id === trackId);
      return { trackId, title: t?.title ?? trackId, artist: t?.artist ?? '', plays: count };
    });

  const perDay = {};
  analyticsEvents.forEach(e => {
    const day = e.ts.slice(0, 10);
    if (!perDay[day]) perDay[day] = { plays: 0, downloads: 0 };
    if (e.event === 'play')     perDay[day].plays++;
    if (e.event === 'download') perDay[day].downloads++;
  });
  const dailyActivity = Object.entries(perDay)
    .sort((a, b) => a[0].localeCompare(b[0]))
    .slice(-7)
    .map(([date, counts]) => ({ date, ...counts }));

  const byUser = {};
  plays.forEach(e => { if (e.userId) byUser[e.userId] = (byUser[e.userId] || 0) + 1; });

  res.json({ totalPlays: plays.length, totalDownloads: downloads.length, topTracks, dailyActivity, byUser });
});

router.get('/track/:id', (req, res) => {
  const events    = analyticsEvents.filter(e => e.trackId === req.params.id);
  const plays     = events.filter(e => e.event === 'play').length;
  const downloads = events.filter(e => e.event === 'download').length;
  res.json({ trackId: req.params.id, plays, downloads, events });
});

module.exports = router;
