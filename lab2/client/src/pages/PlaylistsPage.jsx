import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { usePlayer } from '../context/PlayerContext';
import TrackCard from '../components/TrackCard';

export default function PlaylistsPage() {
  const { user } = useAuth();
  const { play } = usePlayer();
  const [playlists, setPlaylists] = useState([]);
  const [name, setName]           = useState('');
  const [open, setOpen]           = useState(null);

  const load = () => {
    if (!user) return;
    fetch(`/api/playlists?userId=${user.id}`)
      .then(r => r.json())
      .then(setPlaylists);
  };

  useEffect(load, [user]);

  const create = async () => {
    if (!name.trim()) return;
    await fetch('/api/playlists', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: user.id, name }),
    });
    setName('');
    load();
  };

  const remove = async (id) => {
    await fetch(`/api/playlists/${id}`, { method: 'DELETE' });
    if (open === id) setOpen(null);
    load();
  };

  const removeTrack = async (playlistId, trackId) => {
    await fetch(`/api/playlists/${playlistId}/tracks/${trackId}`, { method: 'DELETE' });
    load();
  };

  if (!user) return (
    <div style={{ maxWidth: 500, margin: '60px auto', textAlign: 'center' }}>
      <p style={{ color: 'var(--text-dim)', marginBottom: 16 }}>Увійдіть, щоб переглядати плейлисти.</p>
    </div>
  );

  const currentPl = playlists.find(p => p.id === open);

  return (
    <div style={{ maxWidth: 760, margin: '0 auto' }}>
      <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 16 }}>Мої плейлисти</h1>

      {/* Create */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <input
          placeholder="Назва плейлиста..."
          value={name}
          onChange={e => setName(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && create()}
          style={{ maxWidth: 280 }}
        />
        <button onClick={create} style={{ background: 'var(--accent)', color: '#fff' }}>Створити</button>
      </div>

      {playlists.length === 0 && <p style={{ color: 'var(--text-dim)' }}>Плейлистів ще немає.</p>}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {playlists.map(pl => (
          <div key={pl.id} style={{
            background: 'var(--surface)', border: '1px solid var(--border)',
            borderRadius: 'var(--radius)', overflow: 'hidden',
          }}>
            {/* Header */}
            <div
              onClick={() => setOpen(open === pl.id ? null : pl.id)}
              style={{
                display: 'flex', alignItems: 'center', gap: 10,
                padding: '12px 14px', cursor: 'pointer',
              }}
            >
              <span style={{ flex: 1, fontWeight: 600 }}>{pl.name}</span>
              <span style={{ color: 'var(--text-dim)', fontSize: 12 }}>
                {pl.tracks?.length ?? 0} треків
              </span>
              {pl.tracks?.length > 0 && (
                <button
                  onClick={e => { e.stopPropagation(); play(pl.tracks, 0); }}
                  style={{ background: 'var(--accent)', color: '#fff', fontSize: 12, padding: '4px 10px' }}
                >
                  ▶ Грати
                </button>
              )}
              <button
                onClick={e => { e.stopPropagation(); remove(pl.id); }}
                style={{ background: 'transparent', color: 'var(--danger)', fontSize: 16, padding: '2px 6px' }}
              >
                ×
              </button>
              <span style={{ color: 'var(--text-dim)' }}>{open === pl.id ? '▲' : '▼'}</span>
            </div>

            {/* Tracks */}
            {open === pl.id && (
              <div style={{ padding: '0 14px 14px', display: 'flex', flexDirection: 'column', gap: 8 }}>
                {pl.tracks?.length === 0 && (
                  <p style={{ color: 'var(--text-dim)', fontSize: 13 }}>Порожній плейлист. Додайте треки зі сторінки Треки.</p>
                )}
                {pl.tracks?.map(t => (
                  <div key={t.id} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <div style={{ flex: 1 }}>
                      <TrackCard track={t} />
                    </div>
                    <button
                      onClick={() => removeTrack(pl.id, t.id)}
                      style={{ background: 'transparent', color: 'var(--danger)', fontSize: 16, flexShrink: 0 }}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
