import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi, extractErrorMessage } from '../api';
import { useAuth } from '../context/AuthContext';
import { getRoleHome } from '../utils/app';

export default function SetupPasswordPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, user } = useAuth();

  const email = location.state?.email || user?.email || '';

  const [newPw, setNewPw]         = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [loading, setLoading]     = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (newPw !== confirmPw) { toast.error('Passwords do not match'); return; }

    setLoading(true);
    try {
      const { data } = await authApi.setupPassword({ newPassword: newPw });
      login(data, { email });
      toast.success('Password set! Welcome to Festiva 🎵');
      navigate(getRoleHome(data.role));
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Could not set password'));
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

        <div className="auth-title">Set your password</div>
        <div className="auth-subtitle">
          Welcome! Please set a new password for <strong>{email}</strong> to continue.
        </div>

        <form onSubmit={onSubmit}>
          <div className="form-group">
            <label className="form-label">New password</label>
            <input
              className="input"
              type="password"
              minLength={8}
              placeholder="Min 8 characters"
              value={newPw}
              onChange={e => setNewPw(e.target.value)}
              autoFocus
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Confirm password</label>
            <input
              className="input"
              type="password"
              minLength={8}
              placeholder="Repeat password"
              value={confirmPw}
              onChange={e => setConfirmPw(e.target.value)}
              required
            />
          </div>

          <button
            className="btn btn-primary w-full"
            style={{ justifyContent: 'center' }}
            type="submit"
            disabled={loading || newPw.length < 8}
          >
            {loading ? <span className="spinner" /> : 'Set Password & Continue'}
          </button>
        </form>
      </div>
    </div>
  );
}
