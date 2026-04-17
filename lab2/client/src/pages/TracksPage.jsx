import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { usePlayer } from '../context/PlayerContext';
import TrackCard from '../components/TrackCard';
import ReviewsPanel from '../components/ReviewsPanel';

export default function TracksPage() {
  const { user } = useAuth();
  const { play } = usePlayer();
  const [tracks, setTracks]   = useState([]);
  const [genres, setGenres]   = useState([]);
  const [genre, setGenre]     = useState('');
  const [q, setQ]             = useState('');
  const [selected, setSelected] = useState(null);   // for reviews
  const [playlists, setPlaylists] = useState([]);
  const [addModal, setAddModal] = useState(null);   // track to add

  useEffect(() => {
    fetch('/api/tracks/genres').then(r => r.json()).then(setGenres);
  }, []);

  useEffect(() => {
    const params = new URLSearchParams();
    if (genre) params.set('genre', genre);
    if (q)     params.set('q', q);
    fetch(`/api/tracks?${params}`).then(r => r.json()).then(setTracks);
  }, [genre, q]);

  useEffect(() => {
    if (user) fetch(`/api/playlists?userId=${user.id}`).then(r => r.json()).then(setPlaylists);
  }, [user]);

  const addToPlaylist = async (playlistId, trackId) => {
    await fetch(`/api/playlists/${playlistId}/tracks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ trackId }),
    });
    setAddModal(null);
  };

  return (
    <div style={{ maxWidth: 760, margin: '0 auto' }}>
      <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 16 }}>Треки</h1>

      {/* Filters */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
        <input
          placeholder="Пошук..."
          value={q}
          onChange={e => setQ(e.target.value)}
          style={{ maxWidth: 220 }}
        />
        <select
          value={genre}
          onChange={e => setGenre(e.target.value)}
          style={{
            background: 'var(--surface2)', border: '1px solid var(--border)',
            borderRadius: 6, color: 'var(--text)', padding: '7px 10px', fontSize: 13,
          }}
        >
          <option value="">Всі жанри</option>
          {genres.map(g => <option key={g}>{g}</option>)}
        </select>
        {tracks.length > 0 && (
          <button
            onClick={() => play(tracks, 0)}
            style={{ background: 'var(--accent)', color: '#fff', marginLeft: 'auto' }}
          >
            ▶ Грати всі
          </button>
        )}
      </div>

      {/* Track list */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {tracks.map(t => (
          <TrackCard
            key={t.id}
            track={t}
            onAddToPlaylist={user ? () => setAddModal(t) : undefined}
          />
        ))}
      </div>

      {/* Reviews panel */}
      {selected && (
        <ReviewsPanel track={selected} onClose={() => setSelected(null)} />
      )}

      {/* Add-to-playlist modal */}
      {addModal && (
        <Modal title={`Додати «${addModal.title}» до плейлиста`} onClose={() => setAddModal(null)}>
          {playlists.length === 0
            ? <p style={{ color: 'var(--text-dim)' }}>У вас немає плейлистів. Створіть на сторінці Плейлисти.</p>
            : playlists.map(pl => (
              <div
                key={pl.id}
                onClick={() => addToPlaylist(pl.id, addModal.id)}
                style={{
                  padding: '10px 12px', borderRadius: 8,
                  background: 'var(--surface2)', border: '1px solid var(--border)',
                  cursor: 'pointer', marginBottom: 8,
                }}
              >
                {pl.name} <span style={{ color: 'var(--text-dim)', fontSize: 12 }}>({pl.trackIds?.length ?? 0} треків)</span>
              </div>
            ))
          }
        </Modal>
      )}

      {/* Reviews link per track */}
      <div style={{ marginTop: 24 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 10, color: 'var(--text-dim)' }}>
          Відгуки до треків
        </h2>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {tracks.map(t => (
            <button
              key={t.id}
              onClick={() => setSelected(t)}
              style={{ background: 'var(--surface2)', color: 'var(--text-dim)', fontSize: 12 }}
            >
              {t.title}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

function Modal({ title, onClose, children }) {
  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,.6)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100,
    }} onClick={onClose}>
      <div style={{
        background: 'var(--surface)', border: '1px solid var(--border)',
        borderRadius: 12, padding: 24, minWidth: 300, maxWidth: 420,
      }} onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <strong>{title}</strong>
          <button onClick={onClose} style={{ background: 'transparent', color: 'var(--text-dim)', fontSize: 18 }}>×</button>
        </div>
        {children}
      </div>
    </div>
  );
}
