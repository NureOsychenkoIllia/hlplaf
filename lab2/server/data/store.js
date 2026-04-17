const { v4: uuidv4 } = require('uuid');

const tracks = [
  { id: '1', title: 'Jet Stream Heart',       artist: 'Temples',           genre: 'Psychedelic Rock', duration: 30,  url: '/static/music/Temples - Jet Stream Heart.flac' },
  { id: '2', title: 'MODERATE TALKING',       artist: 'Eccentric',         genre: 'Electronic',       duration: 270, url: '/static/music/Eccentric - MODERATE TALKING.flac' },
  { id: '3', title: 'Happy Song',             artist: 'Bring Me The Horizon', genre: 'Rock',          duration: 30,  url: '/static/music/Bring Me The Horizon - Happy Song.flac' },
  { id: '4', title: 'Never Gonna Give You Up', artist: 'Rick Astley',      genre: 'Pop',              duration: 213, url: '/static/music/Rick Astley - Never Gonna Give You Up.opus' },
  { id: '5', title: 'Genesis 22:10',          artist: 'The Binding of Isaac Title Song', genre: 'Soundtrack',   duration: 139, url: '/static/music/The Binding of Issac Title Theme - Genesis 22:10' },
  { id: '6', title: 'Given Up',               artist: 'Linkin Park',       genre: 'Rock',             duration: 189, url: '/static/music/Linkin Park - Given Up.opus' },
  { id: '7', title: 'The Emptiness Machine',  artist: 'Linkin Park',       genre: 'Rock',             duration: 191, url: '/static/music/Linkin Park - The Emptiness Machine (Audio) [8T2UfcdlAUE].opus' },
  { id: '8', title: 'FMB',                    artist: 'Minimal Schlager',  genre: 'Electronic',       duration: 189, url: '/static/music/Minimal Schlager - FMB (Official Audio) [qfO_ezbd9xE].opus' },
  { id: '9', title: 'Tally', artist: 'Twenty One Pilots', genre: 'Alternative', duration: 213, url: '/static/music/Twenty One Pilots - Tally (Official Audio) [mbXvhmGwumw].opus' },
  { id: '10', title: 'Say It Right', artist: 'Nelly Furtado', genre: 'Indie', duration: 223, url: '/static/music/Nelly Furtado - Say It Right [UyHUvm8T6CY].opus' }
];

const users = [
  { id: 'u1', username: 'alice', password: 'pass', avatar: 'A', playlists: [] },
  { id: 'u2', username: 'bob',   password: 'pass', avatar: 'B', playlists: [] },
];

const playlists = [
  { id: 'p1', userId: 'u1', name: 'Chill Vibes', trackIds: ['1', '3', '9'] },
  { id: 'p2', userId: 'u1', name: 'Focus Mode',  trackIds: ['2', '5'] },
  { id: 'p3', userId: 'u2', name: 'Night Drive',  trackIds: ['4', '6', '7'] },
];

const reviews = [
  { id: 'r1', trackId: '1', userId: 'u1', rating: 5, text: 'Absolute banger for night drives.' },
  { id: 'r2', trackId: '2', userId: 'u2', rating: 4, text: 'Super relaxing, perfect for studying.' },
];

const analyticsEvents = [];

module.exports = { tracks, users, playlists, reviews, analyticsEvents, uuidv4 };
