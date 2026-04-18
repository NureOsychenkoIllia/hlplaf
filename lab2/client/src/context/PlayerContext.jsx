import { createContext, useContext, useState } from 'react';

const PlayerContext = createContext(null);

const postEvent = (trackId, event, userId) =>
  fetch(`/api/tracks/${trackId}/${event}`, {
    method: 'POST',
    headers: userId ? { 'x-user-id': userId } : {},
  }).catch(() => {});

export function PlayerProvider({ userId, children }) {
  const [queue, setQueue]     = useState([]);
  const [currentIdx, setIdx]  = useState(0);
  const [playing, setPlaying] = useState(false);

  const current = queue[currentIdx] || null;

  const play = (tracks, idx = 0) => {
    setQueue(tracks);
    setIdx(idx);
    setPlaying(true);
    if (tracks[idx]) postEvent(tracks[idx].id, 'play', userId);
  };

  const playTrack = (track) => play([track], 0);

  const next = () => {
    if (currentIdx < queue.length - 1) {
      const ni = currentIdx + 1;
      setIdx(ni);
      postEvent(queue[ni].id, 'play', userId);
    }
  };

  const prev   = () => { if (currentIdx > 0) setIdx(i => i - 1); };
  const toggle = () => setPlaying(p => !p);

  return (
    <PlayerContext.Provider value={{ current, queue, currentIdx, playing, play, playTrack, next, prev, toggle, userId }}>
      {children}
    </PlayerContext.Provider>
  );
}

export const usePlayer = () => useContext(PlayerContext);
