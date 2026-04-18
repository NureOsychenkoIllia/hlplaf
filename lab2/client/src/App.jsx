import { BrowserRouter, Routes, Route, NavLink, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { PlayerProvider } from './context/PlayerContext';
import Player from './components/Player';
import TracksPage from './pages/TracksPage';
import PlaylistsPage from './pages/PlaylistsPage';
import ProfilePage from './pages/ProfilePage';
import AnalyticsPage from './pages/AnalyticsPage';
import AuthPage from './pages/AuthPage';

function Layout() {
  const { user, logout } = useAuth();

  return (
    <PlayerProvider userId={user?.id}>
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <nav style={{
        display: 'flex', alignItems: 'center', gap: 6,
        padding: '0 20px', height: 52,
        background: 'var(--surface)', borderBottom: '1px solid var(--border)',
        flexShrink: 0,
      }}>
        <span style={{ fontWeight: 700, fontSize: 16, color: 'var(--accent)', marginRight: 16 }}>
          VeryPromisingMusicPlayer
        </span>
        <NavLink to="/tracks"    style={navStyle}>Треки</NavLink>
        <NavLink to="/playlists" style={navStyle}>Плейлисти</NavLink>
        {user && <NavLink to="/profile"   style={navStyle}>Профіль</NavLink>}
        <NavLink to="/analytics" style={navStyle}>Аналітика</NavLink>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center' }}>
          {user ? (
            <>
              <span style={{ color: 'var(--text-dim)' }}>{user.username}</span>
              <button onClick={logout} style={{ background: 'var(--surface2)', color: 'var(--text-dim)' }}>
                Вийти
              </button>
            </>
          ) : (
            <NavLink to="/auth">
              <button style={{ background: 'var(--accent)', color: '#fff' }}>Увійти</button>
            </NavLink>
          )}
        </div>
      </nav>

      <div style={{ flex: 1, overflow: 'auto', padding: '24px 20px' }}>
        <Routes>
          <Route path="/"          element={<Navigate to="/tracks" replace />} />
          <Route path="/tracks"    element={<TracksPage />} />
          <Route path="/playlists" element={<PlaylistsPage />} />
          <Route path="/profile"   element={user ? <ProfilePage /> : <Navigate to="/auth" />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/auth"      element={<AuthPage />} />
        </Routes>
      </div>

      <Player />
    </div>
    </PlayerProvider>
  );
}

const navStyle = ({ isActive }) => ({
  padding: '4px 10px',
  borderRadius: 6,
  color: isActive ? 'var(--accent)' : 'var(--text-dim)',
  fontWeight: isActive ? 600 : 400,
  background: isActive ? 'rgba(124,106,247,.12)' : 'transparent',
});

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Layout />
      </BrowserRouter>
    </AuthProvider>
  );
}
