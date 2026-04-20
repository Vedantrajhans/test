import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi, extractErrorMessage } from '../api';
import { useAuth } from '../context/AuthContext';
import { getRoleHome } from '../utils/app';

export default function MfaVerifyPage() {
  const [code, setCode]   = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const { state } = useLocation();

  const email      = state?.email      || '';
  const mfaToken   = state?.mfaToken   || '';
  const firstLogin = state?.firstLogin || false;

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await authApi.verifyMfa({ email, mfaToken, code });

      // If it's a first-login organizer, go to setup-password page
      // Pass the mfaToken so setup-password can validate the OTP challenge
      if (data.firstLoginRequired) {
        login(data, { email }); // temp JWT so the setup-password endpoint works
        navigate('/setup-password', { state: { email, mfaToken } });
        return;
      }

      login(data, { email });
      toast.success('Verified! Welcome back 🎵');
      navigate(getRoleHome(data.role));
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Invalid OTP code'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <div className="auth-brand">
          <div style={{ width: 30, height: 30, borderRadius: 8, background: 'var(--accent)', display: 'grid', placeItems: 'center', fontSize: 14 }}>🎵</div>
          Festiva
        </div>

        <div className="auth-title">Verify your identity</div>
        <div className="auth-subtitle">
          A 6-digit OTP was sent to <strong>{email}</strong>. Enter it below to continue.
        </div>

        <form onSubmit={onSubmit}>
          <div className="form-group">
            <label className="form-label">OTP Code</label>
            <input
              className="input"
              style={{ fontSize: '1.4rem', textAlign: 'center', letterSpacing: '0.35em' }}
              maxLength={6}
              placeholder="000000"
              value={code}
              onChange={e => setCode(e.target.value.replace(/\D/g, ''))}
              autoFocus
              required
            />
          </div>
          <button
            className="btn btn-primary w-full"
            style={{ justifyContent: 'center' }}
            type="submit"
            disabled={loading || code.length !== 6}
          >
            {loading ? <span className="spinner" /> : 'Verify OTP'}
          </button>
        </form>

        <div style={{ marginTop: 14, fontSize: 12, color: 'var(--muted2)', textAlign: 'center' }}>
          Didn't receive the code? Check your spam folder, or{' '}
          <button
            style={{ background: 'none', border: 'none', color: 'var(--accent2)', cursor: 'pointer', fontSize: 12, fontWeight: 600 }}
            onClick={() => navigate('/login')}
          >
            go back and try again
          </button>
        </div>
      </div>
    </div>
  );
}
