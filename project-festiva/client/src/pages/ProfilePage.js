import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import { authApi, extractErrorMessage } from '../api';
import Sidebar from '../components/common/Sidebar';

export default function ProfilePage() {
  const { user } = useAuth();
  const [form, setForm] = useState({ firstName: '', lastName: '', phone: '' });
  const [profileLoading, setProfileLoading] = useState(false);
  const [tab, setTab] = useState('profile');

  const [pwStep, setPwStep]       = useState('form');
  const [pwForm, setPwForm]       = useState({ current: '', next: '', confirm: '' });
  const [mfaToken, setMfaToken]   = useState('');
  const [otpCode, setOtpCode]     = useState('');
  const [pwLoading, setPwLoading] = useState(false);

  // Fix #27: load actual profile data on mount
  useEffect(() => {
    authApi.getProfile()
      .then(({ data }) => {
        setForm({
          firstName: data.firstName || '',
          lastName: data.lastName || '',
          phone: data.phone || '',
        });
      })
      .catch(() => {
        // Profile endpoint might not be implemented yet — fall back to empty
      });
  }, []);

  const set   = k => e => setForm(f => ({ ...f, [k]: e.target.value }));
  const setPw = k => e => setPwForm(f => ({ ...f, [k]: e.target.value }));

  // Fix #27: actually call the API to save profile
  const saveProfile = async (e) => {
    e.preventDefault();
    setProfileLoading(true);
    try {
      await authApi.updateProfile({
        firstName: form.firstName,
        lastName: form.lastName,
        phone: form.phone,
      });
      toast.success('Profile updated successfully!');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Could not update profile'));
    } finally {
      setProfileLoading(false);
    }
  };

  const requestOtp = async (e) => {
    e.preventDefault();
    if (pwForm.next !== pwForm.confirm) { toast.error('Passwords do not match'); return; }
    if (pwForm.next.length < 8) { toast.error('Password must be at least 8 characters'); return; }
    setPwLoading(true);
    try {
      const { data } = await authApi.requestChangePasswordOtp();
      setMfaToken(data.mfaToken);
      setPwStep('otp');
      toast.success('OTP sent to your email');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Could not send OTP'));
    } finally {
      setPwLoading(false);
    }
  };

  const confirmChange = async (e) => {
    e.preventDefault();
    setPwLoading(true);
    try {
      await authApi.changePassword({
        currentPassword: pwForm.current,
        newPassword: pwForm.next,
        mfaToken,
        otpCode,
      });
      toast.success('Password changed successfully!');
      setPwStep('form');
      setPwForm({ current: '', next: '', confirm: '' });
      setOtpCode('');
      setMfaToken('');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Could not change password'));
    } finally {
      setPwLoading(false);
    }
  };

  const initials  = (u) => (u?.email || '??').slice(0, 2).toUpperCase();
  const roleLabel = { ORGANIZER: 'Organizer', PROMOTER: 'Promoter', PRODUCER: 'Producer', ATTENDEE: 'Attendee' };

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

          <div className="flex mb-24" style={{ gap: 14 }}>
            <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'linear-gradient(135deg, var(--accent), var(--purple))', display: 'grid', placeItems: 'center', fontSize: 20, fontWeight: 700, color: 'white', flexShrink: 0 }}>
              {initials(user)}
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: 15 }}>{user?.email}</div>
              <span className="badge badge-accent mt-4">{roleLabel[user?.role] || user?.role}</span>
            </div>
          </div>

          <div className="flex mb-20" style={{ gap: 4 }}>
            {[['profile', 'Edit Profile'], ['password', 'Change Password']].map(([t, lbl]) => (
              <button
                key={t}
                onClick={() => { setTab(t); setPwStep('form'); }}
                style={{
                  padding: '8px 16px', borderRadius: 8,
                  background: tab === t ? 'var(--accent)' : 'rgba(255,255,255,0.04)',
                  border: '1px solid var(--border2)',
                  color: tab === t ? 'white' : 'var(--muted)',
                  fontWeight: 600, fontSize: 13, cursor: 'pointer',
                }}
              >
                {lbl}
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
                  <label className="form-label">Email (read-only)</label>
                  <input className="input" value={user?.email || ''} disabled style={{ opacity: 0.5 }} />
                </div>
                <button className="btn btn-primary" type="submit" disabled={profileLoading}>
                  {profileLoading ? <span className="spinner" /> : 'Save changes'}
                </button>
              </form>
            )}

            {tab === 'password' && pwStep === 'form' && (
              <form onSubmit={requestOtp}>
                <div className="form-group">
                  <label className="form-label">Current password</label>
                  <input className="input" type="password" value={pwForm.current} onChange={setPw('current')} placeholder="Current password" required />
                </div>
                <div className="form-group">
                  <label className="form-label">New password</label>
                  <input className="input" type="password" minLength={8} value={pwForm.next} onChange={setPw('next')} placeholder="Min 8 characters" required />
                </div>
                <div className="form-group">
                  <label className="form-label">Confirm new password</label>
                  <input className="input" type="password" minLength={8} value={pwForm.confirm} onChange={setPw('confirm')} placeholder="Repeat new password" required />
                </div>
                <button className="btn btn-primary" type="submit" disabled={pwLoading}>
                  {pwLoading ? <span className="spinner" /> : 'Send OTP & Continue'}
                </button>
              </form>
            )}

            {tab === 'password' && pwStep === 'otp' && (
              <form onSubmit={confirmChange}>
                <div style={{ marginBottom: 16, padding: '10px 14px', background: 'rgba(255,255,255,0.04)', borderRadius: 8, fontSize: 13, color: 'var(--muted)', lineHeight: 1.6 }}>
                  A 6-digit OTP was sent to <strong>{user?.email}</strong>. Enter it below to confirm your password change.
                </div>
                <div className="form-group">
                  <label className="form-label">OTP Code</label>
                  <input
                    className="input"
                    style={{ fontSize: '1.3rem', textAlign: 'center', letterSpacing: '0.3em' }}
                    maxLength={6}
                    placeholder="000000"
                    value={otpCode}
                    onChange={e => setOtpCode(e.target.value.replace(/\D/g, ''))}
                    autoFocus
                    required
                  />
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button
                    type="button"
                    className="btn"
                    style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border2)', color: 'var(--muted)' }}
                    onClick={() => { setPwStep('form'); setOtpCode(''); }}
                  >
                    Back
                  </button>
                  <button className="btn btn-primary" type="submit" disabled={pwLoading || otpCode.length !== 6} style={{ flex: 1, justifyContent: 'center' }}>
                    {pwLoading ? <span className="spinner" /> : 'Confirm Change'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
