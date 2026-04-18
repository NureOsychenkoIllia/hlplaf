import { useEffect, useRef } from 'react';
import { usePlayer } from '../context/PlayerContext';

function fmt(s) {
  if (!s || isNaN(s)) return '0:00';
  return `${Math.floor(s / 60)}:${Math.floor(s % 60).toString().padStart(2, '0')}`;
}

export default function Player() {
  const { current, playing, toggle, next, prev, queue, currentIdx, userId } = usePlayer();
  const audioRef = useRef(null);

  useEffect(() => {
    if (!audioRef.current || !current?.url) return;
    audioRef.current.src = current.url;
    if (playing) audioRef.current.play().catch(() => {});
  }, [current]);

  useEffect(() => {
    if (!audioRef.current) return;
    if (playing) audioRef.current.play().catch(() => {});
    else         audioRef.current.pause();
  }, [playing]);

  if (!current) return null;

  return (
    <div style={{
      position: 'sticky', bottom: 0,
      background: 'var(--surface)', borderTop: '1px solid var(--border)',
      padding: '10px 20px',
      display: 'flex', alignItems: 'center', gap: 16,
      flexShrink: 0,
    }}>
      <audio ref={audioRef} onEnded={next} />

      <div style={{ minWidth: 160 }}>
        <div style={{ fontWeight: 600, fontSize: 13 }}>{current.title}</div>
        <div style={{ color: 'var(--text-dim)', fontSize: 12 }}>{current.artist}</div>
      </div>

      <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
        <CtrlBtn onClick={prev} disabled={currentIdx === 0}>⏮</CtrlBtn>
        <button
          onClick={toggle}
          style={{
            background: 'var(--accent)', color: '#fff',
            width: 34, height: 34, borderRadius: '50%',
            fontSize: 15, display: 'flex', alignItems: 'center', justifyContent: 'center',
            padding: 0,
          }}
        >
          {playing ? '⏸' : '▶'}
        </button>
        <CtrlBtn onClick={next} disabled={currentIdx >= queue.length - 1}>⏭</CtrlBtn>
      </div>

      <div style={{ color: 'var(--text-dim)', fontSize: 12 }}>{fmt(current.duration)}</div>

      <div style={{
        marginLeft: 'auto',
        background: 'var(--surface2)', border: '1px solid var(--border)',
        borderRadius: 20, padding: '2px 10px', fontSize: 12, color: 'var(--text-dim)',
      }}>
        {current.genre}
      </div>

      <a
        href={current.url}
        download={`${current.title}.mp3`}
        onClick={() => fetch(`/api/tracks/${current.id}/download`, {
          method: 'POST',
          headers: userId ? { 'x-user-id': userId } : {},
        }).catch(() => {})}
        style={{ textDecoration: 'none' }}
      >
        <button style={{ background: 'var(--surface2)', color: 'var(--text-dim)', fontSize: 12 }}>
          ⬇ Завантажити
        </button>
      </a>
    </div>
  );
}

function CtrlBtn({ children, disabled, onClick }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        background: 'transparent', color: 'var(--text-dim)',
        fontSize: 16, padding: '4px 8px', opacity: disabled ? .3 : 1,
      }}
    >
      {children}
    </button>
  );
}
