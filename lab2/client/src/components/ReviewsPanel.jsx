import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function ReviewsPanel({ track, onClose }) {
  const { user } = useAuth();
  const [reviews, setReviews] = useState([]);
  const [rating, setRating]   = useState(5);
  const [text, setText]       = useState('');
  const [err, setErr]         = useState('');

  const load = () =>
    fetch(`/api/reviews?trackId=${track.id}`)
      .then(r => r.json())
      .then(setReviews);

  useEffect(() => { load(); }, [track.id]);

  const submit = async () => {
    if (!user) return setErr('Увійдіть, щоб залишити відгук');
    setErr('');
    const res = await fetch('/api/reviews', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ trackId: track.id, userId: user.id, rating: Number(rating), text }),
    });
    if (!res.ok) return setErr((await res.json()).error);
    setText('');
    load();
  };

  const remove = async (id) => {
    await fetch(`/api/reviews/${id}`, { method: 'DELETE' });
    load();
  };

  const avg = reviews.length
    ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1)
    : '—';

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,.6)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100,
    }} onClick={onClose}>
      <div style={{
        background: 'var(--surface)', border: '1px solid var(--border)',
        borderRadius: 12, padding: 24, width: 460, maxHeight: '80vh',
        display: 'flex', flexDirection: 'column', gap: 14,
      }} onClick={e => e.stopPropagation()}>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <strong style={{ fontSize: 16 }}>{track.title}</strong>
            <div style={{ color: 'var(--text-dim)', fontSize: 12 }}>{track.artist}</div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ color: '#f5c542', fontWeight: 700 }}>★ {avg}</span>
            <button onClick={onClose} style={{ background: 'transparent', color: 'var(--text-dim)', fontSize: 18 }}>×</button>
          </div>
        </div>

        {/* Reviews list */}
        <div style={{ overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
          {reviews.length === 0 && <p style={{ color: 'var(--text-dim)' }}>Поки немає відгуків.</p>}
          {reviews.map(r => (
            <div key={r.id} style={{
              background: 'var(--surface2)', border: '1px solid var(--border)',
              borderRadius: 8, padding: '10px 12px',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <span style={{ fontWeight: 600, fontSize: 13 }}>{r.author}</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ color: '#f5c542', fontSize: 13 }}>{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</span>
                  {user?.id === r.userId && (
                    <button
                      onClick={() => remove(r.id)}
                      style={{ background: 'transparent', color: 'var(--danger)', fontSize: 12, padding: '2px 4px' }}
                    >
                      ×
                    </button>
                  )}
                </div>
              </div>
              {r.text && <p style={{ fontSize: 13, color: 'var(--text-dim)' }}>{r.text}</p>}
            </div>
          ))}
        </div>

        {/* Add review */}
        {user && (
          <div style={{ borderTop: '1px solid var(--border)', paddingTop: 14, display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <label style={{ color: 'var(--text-dim)', fontSize: 12 }}>Оцінка:</label>
              <select
                value={rating}
                onChange={e => setRating(e.target.value)}
                style={{
                  background: 'var(--surface2)', border: '1px solid var(--border)',
                  borderRadius: 6, color: 'var(--text)', padding: '5px 8px', fontSize: 13, width: 'auto',
                }}
              >
                {[5,4,3,2,1].map(n => <option key={n} value={n}>{n} ★</option>)}
              </select>
            </div>
            <textarea
              rows={2}
              placeholder="Ваш відгук (необов'язково)..."
              value={text}
              onChange={e => setText(e.target.value)}
            />
            {err && <p style={{ color: 'var(--danger)', fontSize: 12 }}>{err}</p>}
            <button onClick={submit} style={{ background: 'var(--accent)', color: '#fff' }}>
              Надіслати
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
