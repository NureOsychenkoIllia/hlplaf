import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { user, login } = useAuth();
  const [profile, setProfile] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [username, setUsername] = useState('');
  const [msg, setMsg]          = useState('');
  const [err, setErr]          = useState('');

  useEffect(() => {
    if (!user) return;
    fetch(`/api/users/${user.id}/profile`).then(r => r.json()).then(p => {
      setProfile(p);
      setUsername(p.username);
    });
    fetch(`/api/reviews?userId=${user.id}`).then(r => r.json()).then(setReviews);
  }, [user]);

  const save = async () => {
    setErr(''); setMsg('');
    const res = await fetch(`/api/users/${user.id}/profile`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username }),
    });
    if (!res.ok) return setErr((await res.json()).error);
    const updated = await res.json();
    setProfile(p => ({ ...p, ...updated }));
    setMsg('Збережено!');
  };

  const delReview = async (id) => {
    await fetch(`/api/reviews/${id}`, { method: 'DELETE' });
    setReviews(rs => rs.filter(r => r.id !== id));
  };

  if (!profile) return <p style={{ color: 'var(--text-dim)' }}>Завантаження...</p>;

  return (
    <div style={{ maxWidth: 560, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
      <h1 style={{ fontSize: 22, fontWeight: 700 }}>Профіль</h1>

      {/* Avatar + stats */}
      <div style={{
        background: 'var(--surface)', border: '1px solid var(--border)',
        borderRadius: 'var(--radius)', padding: 20, display: 'flex', gap: 20, alignItems: 'center',
      }}>
        <div style={{
          width: 56, height: 56, borderRadius: '50%',
          background: 'var(--accent)', display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 22, fontWeight: 700, color: '#fff', flexShrink: 0,
        }}>
          {profile.avatar}
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 17 }}>{profile.username}</div>
          <div style={{ color: 'var(--text-dim)', fontSize: 13, marginTop: 4, display: 'flex', gap: 16 }}>
            <span>Плейлистів: <strong style={{ color: 'var(--text)' }}>{profile.playlistCount}</strong></span>
            <span>Відгуків: <strong style={{ color: 'var(--text)' }}>{profile.reviewCount}</strong></span>
            <span>Прослуховувань: <strong style={{ color: 'var(--text)' }}>{profile.totalPlays}</strong></span>
          </div>
        </div>
      </div>

      {/* Edit username */}
      <div style={{
        background: 'var(--surface)', border: '1px solid var(--border)',
        borderRadius: 'var(--radius)', padding: 20, display: 'flex', flexDirection: 'column', gap: 10,
      }}>
        <h2 style={{ fontSize: 15, fontWeight: 600 }}>Редагувати профіль</h2>
        <input value={username} onChange={e => setUsername(e.target.value)} placeholder="Ім'я користувача" />
        {err && <p style={{ color: 'var(--danger)', fontSize: 12 }}>{err}</p>}
        {msg && <p style={{ color: 'var(--success)', fontSize: 12 }}>{msg}</p>}
        <button onClick={save} style={{ background: 'var(--accent)', color: '#fff', alignSelf: 'flex-start' }}>
          Зберегти
        </button>
      </div>

      {/* My reviews */}
      <div>
        <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 10 }}>Мої відгуки</h2>
        {reviews.length === 0 && <p style={{ color: 'var(--text-dim)' }}>Відгуків ще немає.</p>}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {reviews.map(r => (
            <div key={r.id} style={{
              background: 'var(--surface)', border: '1px solid var(--border)',
              borderRadius: 8, padding: '10px 14px',
              display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
            }}>
              <div>
                <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 2 }}>
                  Трек #{r.trackId}
                  <span style={{ color: '#f5c542', marginLeft: 8, fontWeight: 400 }}>
                    {'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}
                  </span>
                </div>
                {r.text && <p style={{ color: 'var(--text-dim)', fontSize: 13 }}>{r.text}</p>}
              </div>
              <button
                onClick={() => delReview(r.id)}
                style={{ background: 'transparent', color: 'var(--danger)', fontSize: 16, padding: '2px 6px' }}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
