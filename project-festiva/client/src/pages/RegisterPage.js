import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi, extractErrorMessage } from '../api';
import { useAuth } from '../context/AuthContext';
import { savePreferredCity } from '../utils/app';

// Fix #34: consistent inline validation helper
function FieldError({ msg }) {
  if (!msg) return null;
  return <div style={{ color: '#ef4444', fontSize: 11.5, marginTop: 4 }}>{msg}</div>;
}

function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? '' : 'Please enter a valid email address.';
}

export default function RegisterPage() {
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', phone: '', preferredCity: '' });
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const set = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }));

  const touch = (k) => () => {
    setTouched(t => ({ ...t, [k]: true }));
    validateField(k, form[k]);
  };

  const validateField = (k, value) => {
    let err = '';
    if (k === 'email') err = validateEmail(value);
    if (k === 'password' && value && value.length < 8) err = 'Password must be at least 8 characters.';
    if (k === 'firstName' && !value.trim()) err = 'First name is required.';
    if (k === 'lastName' && !value.trim()) err = 'Last name is required.';
    setErrors(e => ({ ...e, [k]: err }));
    return err;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    // Validate all fields on submit
    const newErrors = {
      firstName: form.firstName.trim() ? '' : 'First name is required.',
      lastName: form.lastName.trim() ? '' : 'Last name is required.',
      email: validateEmail(form.email),
      password: form.password.length >= 8 ? '' : 'Password must be at least 8 characters.',
    };
    setErrors(newErrors);
    setTouched({ firstName: true, lastName: true, email: true, password: true });
    if (Object.values(newErrors).some(Boolean)) return;

    setLoading(true);
    try {
      const { data } = await authApi.register(form);
      if (form.preferredCity) savePreferredCity(form.preferredCity);
      login(data, { email: form.email });
      toast.success('Account created!');
      navigate('/concerts');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Registration failed'));
    } finally {
      setLoading(false);
    }
  };

  const inputClass = (k) => `input${touched[k] && errors[k] ? ' input-error' : ''}`;

  return (
    <div className="auth-shell">
      <div className="auth-card" style={{ position: 'relative' }}>
        <button
          onClick={() => navigate('/')}
          style={{
            position: 'absolute', top: 14, right: 14,
            background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border2)',
            borderRadius: 8, width: 30, height: 30, display: 'grid', placeItems: 'center',
            cursor: 'pointer', color: 'var(--muted)', fontSize: 16, lineHeight: 1,
          }}
          title="Back to home"
        >✕</button>

        <div className="auth-brand">
          <div style={{ width: 30, height: 30, borderRadius: 8, background: 'var(--accent)', display: 'grid', placeItems: 'center', fontSize: 14 }}>🎵</div>
          Festiva
        </div>
        <div className="auth-title">Create account</div>
        <div className="auth-subtitle">Join as an attendee to discover and book concerts.</div>

        <form onSubmit={onSubmit} noValidate>
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">First name</label>
              <input className={inputClass('firstName')} value={form.firstName} onChange={set('firstName')} onBlur={touch('firstName')} required />
              <FieldError msg={touched.firstName && errors.firstName} />
            </div>
            <div className="form-group">
              <label className="form-label">Last name</label>
              <input className={inputClass('lastName')} value={form.lastName} onChange={set('lastName')} onBlur={touch('lastName')} required />
              <FieldError msg={touched.lastName && errors.lastName} />
            </div>
          </div>
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
            />
            <FieldError msg={touched.email && errors.email} />
          </div>
          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              className={inputClass('password')}
              type="password"
              placeholder="Min 8 characters"
              minLength={8}
              value={form.password}
              onChange={set('password')}
              onBlur={touch('password')}
              required
            />
            <FieldError msg={touched.password && errors.password} />
          </div>
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">Phone</label>
              <input className="input" type="tel" placeholder="+91 9XXXXXXXXX" value={form.phone} onChange={set('phone')} />
            </div>
            <div className="form-group">
              <label className="form-label">Preferred city</label>
              <input className="input" placeholder="e.g. Mumbai" value={form.preferredCity} onChange={set('preferredCity')} />
            </div>
          </div>
          <button className="btn btn-primary w-full" style={{ justifyContent: 'center', marginTop: 4 }} type="submit" disabled={loading}>
            {loading ? <span className="spinner" /> : 'Create account'}
          </button>
        </form>

        <div className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
