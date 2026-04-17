import { usePlayer } from '../context/PlayerContext';
import { useAuth } from '../context/AuthContext';

function fmt(s) {
  return `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`;
}

export default function TrackCard({ track, onAddToPlaylist }) {
  const { playTrack, toggle, current, playing } = usePlayer();
  const { user } = useAuth();
  const isActive = current?.id === track.id;

  const handlePlay = () => isActive ? toggle() : playTrack(track);

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '10px 14px',
      background: isActive ? 'rgba(124,106,247,.1)' : 'var(--surface)',
      border: `1px solid ${isActive ? 'var(--accent)' : 'var(--border)'}`,
      borderRadius: 'var(--radius)',
      transition: 'border-color .15s, background .15s',
    }}>
      <button
        onClick={handlePlay}
        style={{
          background: isActive ? 'var(--accent)' : 'var(--surface2)',
          color: isActive ? '#fff' : 'var(--text-dim)',
          width: 34, height: 34, borderRadius: '50%',
          fontSize: 14, display: 'flex', alignItems: 'center', justifyContent: 'center',
          padding: 0, flexShrink: 0,
        }}
      >
        {isActive && playing ? '⏸' : '▶'}
      </button>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {track.title}
        </div>
        <div style={{ color: 'var(--text-dim)', fontSize: 12 }}>{track.artist}</div>
      </div>

      <span style={{
        background: 'var(--surface2)', border: '1px solid var(--border)',
        borderRadius: 20, padding: '2px 8px', fontSize: 11, color: 'var(--text-dim)',
        flexShrink: 0,
      }}>
        {track.genre}
      </span>

      <span style={{ color: 'var(--text-dim)', fontSize: 12, flexShrink: 0 }}>{fmt(track.duration)}</span>

      {user && onAddToPlaylist && (
        <button
          onClick={() => onAddToPlaylist(track)}
          title="Додати до плейлиста"
          style={{ background: 'transparent', color: 'var(--text-dim)', padding: '4px 6px', fontSize: 16 }}
        >
          +
        </button>
      )}
    </div>
  );
}
