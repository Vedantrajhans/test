import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { authApi, extractErrorMessage } from '../api';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/common/Sidebar';

export default function AttendeeProfilePage() {
  const { user } = useAuth();
  const [form, setForm] = useState({ firstName: '', lastName: '', phone: '' });
  const [pwForm, setPwForm] = useState({ current: '', next: '', confirm: '' });
  const [mfaEnabled, setMfaEnabled] = useState(false);
  const [tab, setTab] = useState('profile');
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [saving, setSaving] = useState(false);
  const [otpStep, setOtpStep] = useState(false);
  const [otpToken, setOtpToken] = useState('');
  const [otpCode, setOtpCode] = useState('');

  // Fix #8: load existing profile so user sees current values
  useEffect(() => {
    authApi.getProfile()
      .then(({ data }) => {
        setForm({ firstName: data.firstName || '', lastName: data.lastName || '', phone: data.phone || '' });
        setMfaEnabled(data.mfaEnabled || false);
      })
      .catch(() => {})
      .finally(() => setLoadingProfile(false));
  }, []);

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));
  const setPw = k => e => setPwForm(f => ({ ...f, [k]: e.target.value }));

  const saveProfile = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await authApi.updateProfile(form);
      toast.success('Profile updated!');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Failed to update profile'));
    } finally { setSaving(false); }
  };

  const requestPwOtp = async () => {
    try {
      const { data } = await authApi.requestChangePasswordOtp();
      setOtpToken(data.mfaToken);
      setOtpStep(true);
      toast.success('OTP sent to your email');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Failed to send OTP'));
    }
  };

  const changePassword = async (e) => {
    e.preventDefault();
    if (pwForm.next !== pwForm.confirm) { toast.error('Passwords do not match'); return; }
    if (!otpStep) { await requestPwOtp(); return; }
    setSaving(true);
    try {
      await authApi.changePassword({ currentPassword: pwForm.current, newPassword: pwForm.next, mfaToken: otpToken, otpCode });
      toast.success('Password changed successfully!');
      setPwForm({ current: '', next: '', confirm: '' });
      setOtpStep(false); setOtpCode(''); setOtpToken('');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Failed to change password'));
    } finally { setSaving(false); }
  };

  const toggleMfa = async () => {
    try {
      if (mfaEnabled) {
        await authApi.disableMfa();
        setMfaEnabled(false);
        toast.success('MFA disabled');
      } else {
        await authApi.enableMfa();
        setMfaEnabled(true);
        toast.success('MFA enabled — OTP will be sent to your email on login');
      }
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Failed to toggle MFA'));
    }
  };

  const initials = (u) => (u?.email || '??').slice(0, 2).toUpperCase();

  if (loadingProfile) return (
    <div className="app-shell"><Sidebar /><div className="main-content"><div className="page-loader"><div className="spinner" /></div></div></div>
  );

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-content">
        <div className="page-wrap">
          <div className="page-header">
            <div>
              <div className="page-title">Profile</div>
              <div className="page-subtitle">Manage your account details</div>
            </div>
          </div>

          {/* Avatar row */}
          <div className="flex mb-24" style={{ gap: 16 }}>
            <div style={{ width: 60, height: 60, borderRadius: '50%', background: 'linear-gradient(135deg, var(--accent), var(--purple))', display: 'grid', placeItems: 'center', fontSize: 22, fontWeight: 700, color: 'white', flexShrink: 0 }}>
              {initials(user)}
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: 16 }}>{form.firstName} {form.lastName}</div>
              <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 3 }}>
                <span className="badge badge-accent">{user?.role}</span>
                &nbsp;
                <span className={`badge ${mfaEnabled ? 'badge-green' : 'badge-gray'}`}>MFA {mfaEnabled ? 'ON' : 'OFF'}</span>
              </div>
              <div style={{ fontSize: 12, color: 'var(--muted2)', marginTop: 3 }}>{user?.email}</div>
            </div>
          </div>

          {/* Tabs */}
          <div className="flex mb-20" style={{ gap: 4, borderBottom: '1px solid var(--border)', paddingBottom: 0 }}>
            {['profile', 'password', 'security'].map(t => (
              <button key={t} onClick={() => setTab(t)}
                style={{ padding: '8px 16px', borderRadius: '8px 8px 0 0', background: tab === t ? 'var(--bg2)' : 'transparent', border: '1px solid var(--border)', borderBottom: tab === t ? '1px solid var(--bg2)' : '1px solid var(--border)', color: tab === t ? 'var(--text)' : 'var(--muted)', fontWeight: 600, fontSize: 13, marginBottom: -1, cursor: 'pointer' }}>
                {t === 'profile' ? 'Edit Profile' : t === 'password' ? 'Change Password' : 'Security (MFA)'}
              </button>
            ))}
          </div>

          <div className="card card-p" style={{ maxWidth: 480 }}>
            {tab === 'profile' && (
              <form onSubmit={saveProfile}>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label">First name</label>
                    <input className="input" value={form.firstName} onChange={set('firstName')} placeholder="First name" />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Last name</label>
                    <input className="input" value={form.lastName} onChange={set('lastName')} placeholder="Last name" />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">Phone number</label>
                  <input className="input" type="tel" value={form.phone} onChange={set('phone')} placeholder="+91 9XXXXXXXXX" />
                </div>
                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input className="input" value={user?.email || ''} disabled style={{ opacity: 0.5 }} />
                </div>
                <button className="btn btn-primary" type="submit" disabled={saving}>
                  {saving ? <span className="spinner" /> : 'Save changes'}
                </button>
              </form>
            )}

            {tab === 'password' && (
              <form onSubmit={changePassword}>
                <div className="form-group">
                  <label className="form-label">Current password</label>
                  <input className="input" type="password" value={pwForm.current} onChange={setPw('current')} required />
                </div>
                <div className="form-group">
                  <label className="form-label">New password</label>
                  <input className="input" type="password" minLength={8} value={pwForm.next} onChange={setPw('next')} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Confirm new password</label>
                  <input className="input" type="password" minLength={8} value={pwForm.confirm} onChange={setPw('confirm')} required />
                </div>
                {otpStep && (
                  <div className="form-group">
                    <label className="form-label">OTP sent to your email</label>
                    <input className="input" placeholder="6-digit OTP" value={otpCode} onChange={e => setOtpCode(e.target.value)} maxLength={6} />
                  </div>
                )}
                <div className="flex" style={{ gap: 8 }}>
                  <button className="btn btn-primary" type="submit" disabled={saving}>
                    {saving ? <span className="spinner" /> : otpStep ? 'Confirm Change' : 'Get OTP & Change'}
                  </button>
                  {otpStep && (
                    <button type="button" className="btn btn-secondary" onClick={() => { setOtpStep(false); setOtpCode(''); }}>
                      Cancel
                    </button>
                  )}
                </div>
              </form>
            )}

            {tab === 'security' && (
              <div>
                <div style={{ fontWeight: 700, marginBottom: 8 }}>Multi-Factor Authentication (MFA)</div>
                <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 16, lineHeight: 1.6 }}>
                  When MFA is enabled, a one-time password is sent to your email each time you log in.
                  You can also use OTP login from the sign-in page as an alternative to your password.
                </p>
                <div className="flex" style={{ alignItems: 'center', gap: 16, marginBottom: 20 }}>
                  <span className={`badge ${mfaEnabled ? 'badge-green' : 'badge-gray'}`} style={{ fontSize: 13, padding: '5px 12px' }}>
                    MFA is currently {mfaEnabled ? 'ENABLED' : 'DISABLED'}
                  </span>
                  <button className={`btn ${mfaEnabled ? 'btn-danger' : 'btn-primary'}`} onClick={toggleMfa}>
                    {mfaEnabled ? 'Disable MFA' : 'Enable MFA'}
                  </button>
                </div>
                <div className="banner banner-info" style={{ fontSize: 12 }}>
                  💡 Tip: You can also log in with just an OTP (no password needed) from the Login page — select "OTP Login" tab.
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
