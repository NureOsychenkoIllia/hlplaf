import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function AuthPage() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode]       = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr]           = useState('');

  const submit = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      if (mode === 'login') await login(username, password);
      else                  await register(username, password);
      navigate('/tracks');
    } catch (e) {
      setErr(e.message);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <form
        onSubmit={submit}
        style={{
          background: 'var(--surface)', border: '1px solid var(--border)',
          borderRadius: 12, padding: 32, width: 320,
          display: 'flex', flexDirection: 'column', gap: 14,
        }}
      >
        <h2 style={{ fontSize: 18, fontWeight: 700 }}>
          {mode === 'login' ? 'Вхід' : 'Реєстрація'}
        </h2>

        <input
          placeholder="Ім'я користувача"
          value={username}
          onChange={e => setUsername(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Пароль"
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
        />

        {err && <p style={{ color: 'var(--danger)', fontSize: 12 }}>{err}</p>}

        <button type="submit" style={{ background: 'var(--accent)', color: '#fff', padding: '9px' }}>
          {mode === 'login' ? 'Увійти' : 'Зареєструватись'}
        </button>

        <p style={{ textAlign: 'center', color: 'var(--text-dim)', fontSize: 12 }}>
          {mode === 'login' ? 'Немає акаунту?' : 'Вже є акаунт?'}{' '}
          <span
            onClick={() => { setMode(m => m === 'login' ? 'register' : 'login'); setErr(''); }}
            style={{ color: 'var(--accent)', cursor: 'pointer' }}
          >
            {mode === 'login' ? 'Реєстрація' : 'Увійти'}
          </span>
        </p>

        <p style={{ color: 'var(--text-dim)', fontSize: 11, textAlign: 'center' }}>
          Тестові акаунти: alice / pass · bob / pass
        </p>
      </form>
    </div>
  );
}
