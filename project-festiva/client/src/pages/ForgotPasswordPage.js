import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi, extractErrorMessage } from '../api';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [step, setStep]             = useState(1); // 1 = enter email, 2 = enter OTP + new pw
  const [email, setEmail]           = useState('');
  const [mfaToken, setMfaToken]     = useState('');
  const [otpCode, setOtpCode]       = useState('');
  const [newPw, setNewPw]           = useState('');
  const [confirmPw, setConfirmPw]   = useState('');
  const [loading, setLoading]       = useState(false);

  const requestOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await authApi.forgotPassword({ email });
      if (data.mfaToken) {
        setMfaToken(data.mfaToken);
        toast.success('OTP sent to your email');
        setStep(2);
      } else {
        toast.success('If that email exists, an OTP has been sent.');
        setStep(2);
      }
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Failed to send OTP'));
    } finally {
      setLoading(false);
    }
  };

  const resetPassword = async (e) => {
    e.preventDefault();
    if (newPw !== confirmPw) { toast.error('Passwords do not match'); return; }
    setLoading(true);
    try {
      await authApi.resetPassword({ email, mfaToken, otpCode, newPassword: newPw });
      toast.success('Password reset! Please log in.');
      navigate('/login');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Reset failed'));
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

        {step === 1 ? (
          <>
            <div className="auth-title">Forgot password</div>
            <div className="auth-subtitle">Enter your email and we'll send you a one-time code to reset your password.</div>
            <form onSubmit={requestOtp}>
              <div className="form-group">
                <label className="form-label">Email address</label>
                <input
                  className="input"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                />
              </div>
              <button className="btn btn-primary w-full" style={{ justifyContent: 'center' }} type="submit" disabled={loading}>
                {loading ? <span className="spinner" /> : 'Send OTP'}
              </button>
            </form>
          </>
        ) : (
          <>
            <div className="auth-title">Reset password</div>
            <div className="auth-subtitle">
              An OTP was sent to <strong>{email}</strong>. Enter it below with your new password.
            </div>
            <form onSubmit={resetPassword}>
              <div className="form-group">
                <label className="form-label">OTP Code</label>
                <input
                  className="input"
                  style={{ fontSize: '1.3rem', textAlign: 'center', letterSpacing: '0.3em' }}
                  maxLength={6}
                  placeholder="000000"
                  value={otpCode}
                  onChange={e => setOtpCode(e.target.value.replace(/\D/g, ''))}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">New password</label>
                <input className="input" type="password" minLength={8} placeholder="Min 8 characters" value={newPw} onChange={e => setNewPw(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Confirm password</label>
                <input className="input" type="password" minLength={8} placeholder="Repeat password" value={confirmPw} onChange={e => setConfirmPw(e.target.value)} required />
              </div>
              <button
                className="btn btn-primary w-full"
                style={{ justifyContent: 'center' }}
                type="submit"
                disabled={loading || otpCode.length !== 6}
              >
                {loading ? <span className="spinner" /> : 'Reset Password'}
              </button>
            </form>
            <div style={{ marginTop: 12, fontSize: 12, color: 'var(--muted2)', textAlign: 'center' }}>
              Didn't receive it? <button style={{ background: 'none', border: 'none', color: 'var(--accent2)', cursor: 'pointer', fontSize: 12, fontWeight: 600 }} onClick={() => setStep(1)}>Try again</button>
            </div>
          </>
        )}

        <div className="auth-footer">
          <Link to="/login">← Back to login</Link>
        </div>
      </div>
    </div>
  );
}
