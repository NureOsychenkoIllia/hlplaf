import { useEffect, useState } from 'react';

export default function AnalyticsPage() {
  const [data, setData] = useState(null);

  const load = () =>
    fetch('/api/analytics/summary').then(r => r.json()).then(setData);

  useEffect(() => { load(); }, []);

  if (!data) return <p style={{ color: 'var(--text-dim)' }}>Завантаження...</p>;

  const maxPlays = Math.max(...(data.dailyActivity.map(d => d.plays || 0)), 1);

  return (
    <div style={{ maxWidth: 720, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ fontSize: 22, fontWeight: 700 }}>Аналітика</h1>
        <button onClick={load} style={{ background: 'var(--surface2)', color: 'var(--text-dim)', fontSize: 12 }}>
          Оновити
        </button>
      </div>

      {/* Summary cards */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <StatCard label="Всього прослуховувань" value={data.totalPlays} icon="▶" />
        <StatCard label="Всього завантажень"     value={data.totalDownloads} icon="⬇" />
      </div>

      {/* Top tracks */}
      <section>
        <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 10 }}>Топ треків</h2>
        {data.topTracks.length === 0
          ? <p style={{ color: 'var(--text-dim)' }}>Немає даних. Спочатку програйте треки.</p>
          : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {data.topTracks.map((t, i) => (
                <div key={t.trackId} style={{
                  background: 'var(--surface)', border: '1px solid var(--border)',
                  borderRadius: 8, padding: '10px 14px',
                  display: 'flex', alignItems: 'center', gap: 12,
                }}>
                  <span style={{ color: 'var(--text-dim)', fontWeight: 700, width: 20 }}>#{i + 1}</span>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600 }}>{t.title}</div>
                    <div style={{ color: 'var(--text-dim)', fontSize: 12 }}>{t.artist}</div>
                  </div>
                  <span style={{ color: 'var(--accent)', fontWeight: 700 }}>{t.plays} ▶</span>
                </div>
              ))}
            </div>
          )
        }
      </section>

      {/* Daily activity chart */}
      <section>
        <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 10 }}>Активність (останні 7 днів)</h2>
        {data.dailyActivity.length === 0
          ? <p style={{ color: 'var(--text-dim)' }}>Немає даних.</p>
          : (
            <div style={{
              background: 'var(--surface)', border: '1px solid var(--border)',
              borderRadius: 'var(--radius)', padding: '16px 20px',
            }}>
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: 10, height: 100 }}>
                {data.dailyActivity.map(d => (
                  <div key={d.date} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                    <div style={{
                      width: '100%', background: 'var(--accent)',
                      height: `${Math.round(((d.plays || 0) / maxPlays) * 80)}px`,
                      borderRadius: '3px 3px 0 0', minHeight: 2,
                    }} title={`${d.plays || 0} прослуховувань`} />
                    <span style={{ color: 'var(--text-dim)', fontSize: 10 }}>
                      {d.date.slice(5)}
                    </span>
                  </div>
                ))}
              </div>
              <div style={{ color: 'var(--text-dim)', fontSize: 11, marginTop: 6 }}>
                Прослуховування по днях
              </div>
            </div>
          )
        }
      </section>

      {/* Per-user */}
      {Object.keys(data.byUser).length > 0 && (
        <section>
          <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 10 }}>Прослуховування по користувачах</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {Object.entries(data.byUser).map(([uid, count]) => (
              <div key={uid} style={{
                background: 'var(--surface)', border: '1px solid var(--border)',
                borderRadius: 8, padding: '8px 14px',
                display: 'flex', justifyContent: 'space-between',
              }}>
                <span style={{ color: 'var(--text-dim)', fontSize: 13 }}>User {uid}</span>
                <span style={{ fontWeight: 600 }}>{count} ▶</span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function StatCard({ label, value, icon }) {
  return (
    <div style={{
      background: 'var(--surface)', border: '1px solid var(--border)',
      borderRadius: 'var(--radius)', padding: '18px 20px',
    }}>
      <div style={{ color: 'var(--text-dim)', fontSize: 12, marginBottom: 6 }}>{icon} {label}</div>
      <div style={{ fontSize: 28, fontWeight: 700 }}>{value}</div>
    </div>
  );
}
