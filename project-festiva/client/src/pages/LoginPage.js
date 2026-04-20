import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi, extractErrorMessage } from '../api';
import { useAuth } from '../context/AuthContext';
import { getRoleHome } from '../utils/app';

function FieldError({ msg }) {
  if (!msg) return null;
  return <div style={{ color: '#ef4444', fontSize: 11.5, marginTop: 4 }}>{msg}</div>;
}

function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? '' : 'Please enter a valid email address.';
}

export default function LoginPage() {
  const [loginMode, setLoginMode] = useState('password'); // 'password' | 'otp'
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));
  const touch = (k) => () => {
    setTouched(t => ({ ...t, [k]: true }));
    let err = '';
    if (k === 'email') err = validateEmail(form[k]);
    if (k === 'password' && loginMode === 'password' && !form[k]) err = 'Password is required.';
    setErrors(e => ({ ...e, [k]: err }));
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const emailErr = validateEmail(form.email);
    const pwErr = loginMode === 'password' && !form.password ? 'Password is required.' : '';
    setErrors({ email: emailErr, password: pwErr });
    setTouched({ email: true, password: true });
    if (emailErr || pwErr) return;

    setLoading(true);
    try {
      let data;
      if (loginMode === 'otp') {
        // OTP login — request OTP to email, then redirect to MFA verify
        const res = await authApi.requestOtpLogin({ email: form.email });
        data = res.data;
      } else {
        const res = await authApi.login(form);
        data = res.data;
      }

      if (data.mfaRequired) {
        navigate('/mfa-verify', {
          state: { email: form.email, mfaToken: data.mfaToken, firstLogin: data.firstLoginRequired }
        });
        return;
      }

      login(data, { email: form.email });
      toast.success('Welcome back! 🎵');
      navigate(getRoleHome(data.role));
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Login failed'));
    } finally {
      setLoading(false);
    }
  };

  const sessionExpired = searchParams.get('session') === 'expired';
  const inputClass = (k) => `input${touched[k] && errors[k] ? ' input-error' : ''}`;

  return (
    <div className="auth-shell">
      <div className="auth-card" style={{ position: 'relative' }}>
        <button
          onClick={() => navigate('/')}
          style={{ position: 'absolute', top: 14, right: 14, background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border2)', borderRadius: 8, width: 30, height: 30, display: 'grid', placeItems: 'center', cursor: 'pointer', color: 'var(--muted)', fontSize: 16 }}
          title="Back to home"
        >✕</button>

        <div className="auth-brand">
          <div style={{ width: 30, height: 30, borderRadius: 8, background: 'var(--accent)', display: 'grid', placeItems: 'center', fontSize: 14 }}>🎵</div>
          Festiva
        </div>

        <div className="auth-title">Sign in</div>
        <div className="auth-subtitle">Welcome back — use your account to continue.</div>

        {sessionExpired && (
          <div className="banner banner-warn" style={{ marginBottom: 16 }}>
            Your session expired. Please sign in again.
          </div>
        )}

        {/* Login mode toggle */}
        <div style={{ display: 'flex', gap: 4, marginBottom: 20, background: 'var(--bg2)', borderRadius: 10, padding: 4 }}>
          {[['password', '🔑 Password'], ['otp', '📧 OTP Login']].map(([mode, label]) => (
            <button
              key={mode}
              type="button"
              onClick={() => { setLoginMode(mode); setErrors({}); setTouched({}); }}
              style={{
                flex: 1, padding: '7px 0', borderRadius: 8, border: 'none',
                background: loginMode === mode ? 'var(--accent)' : 'transparent',
                color: loginMode === mode ? 'white' : 'var(--muted)',
                fontWeight: 600, fontSize: 13, cursor: 'pointer',
              }}
            >{label}</button>
          ))}
        </div>

        {loginMode === 'otp' && (
          <div className="banner banner-info" style={{ marginBottom: 14, fontSize: 12 }}>
            Enter your email and we'll send a one-time password to your inbox.
          </div>
        )}

        <form onSubmit={onSubmit} noValidate>
          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              className={inputClass('email')}
              type="email"
              placeholder="you@example.com"
              value={form.email}
              onChange={set('email')}
              onBlur={touch('email')}
              required
              autoFocus
            />
            <FieldError msg={touched.email && errors.email} />
          </div>

          {loginMode === 'password' && (
            <div className="form-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 5 }}>
                <label className="form-label" style={{ margin: 0 }}>Password</label>
                <Link to="/forgot-password" style={{ fontSize: 12, color: 'var(--accent2)' }}>Forgot password?</Link>
              </div>
              <input
                className={inputClass('password')}
                type="password"
                placeholder="••••••••"
                value={form.password}
                onChange={set('password')}
                onBlur={touch('password')}
                required
              />
              <FieldError msg={touched.password && errors.password} />
            </div>
          )}

          <button className="btn btn-primary w-full" style={{ justifyContent: 'center', marginTop: 4 }} type="submit" disabled={loading}>
            {loading ? <span className="spinner" /> : loginMode === 'otp' ? 'Send OTP' : 'Sign in'}
          </button>
        </form>

        <div className="auth-footer">
          Don't have an account? <Link to="/register">Register as attendee</Link>
        </div>
      </div>
    </div>
  );
}
