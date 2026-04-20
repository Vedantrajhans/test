import React, { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { extractErrorMessage, producerApi } from '../api';
import { initials } from '../utils/format';
import Sidebar from '../components/common/Sidebar';

const emptyForm = { email: '', firstName: '', lastName: '', password: 'admin12345' };

export default function ProducerDashboard() {
  const [promoters, setPromoters] = useState([]);
  const [allUsers, setAllUsers] = useState([]);
  const [tab, setTab] = useState('promoters');
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const loadPromoters = async () => {
    setLoading(true);
    try {
      const { data } = await producerApi.listPromoters();
      setPromoters(data || []);
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Unable to load promoters'));
    } finally { setLoading(false); }
  };

  const loadAllUsers = async () => {
    setLoading(true);
    try {
      const { data } = await producerApi.listAllUsers({ page: 0, size: 200 });
      setAllUsers(data.content || []);
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Unable to load users'));
    } finally { setLoading(false); }
  };

  useEffect(() => {
    if (tab === 'promoters') loadPromoters();
    else loadAllUsers();
  }, [tab]);

  const filteredPromoters = useMemo(() => promoters.filter(p => {
    const hay = `${p.firstName} ${p.lastName} ${p.email}`.toLowerCase();
    return hay.includes(search.toLowerCase());
  }), [promoters, search]);

  const filteredUsers = useMemo(() => allUsers.filter(u => {
    const hay = `${u.firstName || ''} ${u.lastName || ''} ${u.email} ${u.role}`.toLowerCase();
    return hay.includes(search.toLowerCase());
  }), [allUsers, search]);

  const resetForm = () => { setForm(emptyForm); setEditingId(null); setShowForm(false); };

  const startEdit = (p) => {
    setEditingId(p.id);
    setForm({ email: p.email, firstName: p.firstName, lastName: p.lastName, password: '', status: p.status || 'ACTIVE' });
    setShowForm(true);
    window.scrollTo(0, 0);
  };

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault(); setSaving(true);
    try {
      if (editingId) {
        const { data } = await producerApi.updatePromoter(editingId, { firstName: form.firstName, lastName: form.lastName, status: form.status });
        setPromoters(curr => curr.map(p => p.id === editingId ? data : p));
        toast.success('Promoter updated');
      } else {
        const { data } = await producerApi.createPromoter({ email: form.email, firstName: form.firstName, lastName: form.lastName, password: form.password });
        setPromoters(curr => [data, ...curr]);
        toast.success('Promoter created');
      }
      resetForm();
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Save failed'));
    } finally { setSaving(false); }
  };

  // Soft deactivate
  const onDeactivate = async (id) => {
    if (!window.confirm('Deactivate this promoter?')) return;
    try {
      await producerApi.deactivatePromoter(id);
      setPromoters(curr => curr.map(p => p.id === id ? { ...p, status: 'INACTIVE' } : p));
      toast.success('Promoter deactivated');
    } catch (err) { toast.error(extractErrorMessage(err, 'Failed')); }
  };

  // Hard delete promoter
  const onHardDeletePromoter = async (id, email) => {
    if (!window.confirm(`PERMANENTLY DELETE promoter "${email}"? This cannot be undone.`)) return;
    try {
      await producerApi.hardDeletePromoter(id);
      setPromoters(curr => curr.filter(p => p.id !== id));
      toast.success('Promoter permanently deleted');
    } catch (err) { toast.error(extractErrorMessage(err, 'Failed')); }
  };

  // Hard delete any user
  const onHardDeleteUser = async (id, email) => {
    if (!window.confirm(`PERMANENTLY DELETE user "${email}"? This cannot be undone.`)) return;
    try {
      await producerApi.hardDeleteUser(id);
      setAllUsers(curr => curr.filter(u => u.id !== id));
      toast.success('User permanently deleted');
    } catch (err) { toast.error(extractErrorMessage(err, 'Failed')); }
  };

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-content">
        <div className="page-wrap">
          <div className="page-header">
            <div>
              <div className="page-title">Producer Dashboard</div>
              <div className="page-subtitle">Manage promoters and platform users</div>
            </div>
            {tab === 'promoters' && (
              <button className="btn btn-primary" onClick={() => { setEditingId(null); setForm(emptyForm); setShowForm(true); }}>+ Add Promoter</button>
            )}
          </div>

          {/* Stats */}
          <div className="stat-row">
            <div className="stat-card"><div className="stat-label">Total Promoters</div><div className="stat-value">{promoters.length}</div></div>
            <div className="stat-card"><div className="stat-label">Active</div><div className="stat-value" style={{ color: 'var(--green)' }}>{promoters.filter(p => p.status === 'ACTIVE').length}</div></div>
            <div className="stat-card"><div className="stat-label">Inactive</div><div className="stat-value" style={{ color: 'var(--red)' }}>{promoters.filter(p => p.status !== 'ACTIVE').length}</div></div>
            <div className="stat-card"><div className="stat-label">All Users</div><div className="stat-value">{allUsers.length}</div></div>
          </div>

          {/* Tabs */}
          <div className="flex mb-16" style={{ gap: 4, borderBottom: '1px solid var(--border)' }}>
            {[['promoters','Promoters'],['users','All Users']].map(([t, label]) => (
              <button key={t} onClick={() => { setTab(t); setSearch(''); setShowForm(false); }}
                style={{ padding: '8px 18px', borderRadius: '8px 8px 0 0', background: tab===t ? 'var(--bg2)' : 'transparent', border: '1px solid var(--border)', borderBottom: tab===t ? '1px solid var(--bg2)' : '1px solid var(--border)', color: tab===t ? 'var(--text)' : 'var(--muted)', fontWeight: 600, fontSize: 13, marginBottom: -1, cursor: 'pointer' }}>
                {label}
              </button>
            ))}
          </div>

          {/* PROMOTER FORM */}
          {tab === 'promoters' && showForm && (
            <div className="card card-p mb-16" style={{ maxWidth: 520 }}>
              <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>{editingId ? 'Edit Promoter' : 'Add Promoter'}</div>
              <form onSubmit={onSubmit}>
                <div className="grid-2">
                  <div className="form-group"><label className="form-label">First name</label><input className="input" value={form.firstName} onChange={set('firstName')} required /></div>
                  <div className="form-group"><label className="form-label">Last name</label><input className="input" value={form.lastName} onChange={set('lastName')} required /></div>
                </div>
                <div className="form-group"><label className="form-label">Email</label><input className="input" type="email" value={form.email} disabled={Boolean(editingId)} onChange={set('email')} required /></div>
                {!editingId && <div className="form-group"><label className="form-label">Password</label><input className="input" type="password" minLength={8} value={form.password} onChange={set('password')} required /></div>}
                {editingId && (
                  <div className="form-group">
                    <label className="form-label">Status</label>
                    <select className="input select" value={form.status} onChange={set('status')}>
                      <option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option><option value="SUSPENDED">Suspended</option>
                    </select>
                  </div>
                )}
                <div className="flex" style={{ gap: 8 }}>
                  <button type="button" className="btn btn-secondary" onClick={resetForm}>Cancel</button>
                  <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? <span className="spinner" /> : editingId ? 'Save changes' : 'Create promoter'}</button>
                </div>
              </form>
            </div>
          )}

          {/* LIST */}
          <div className="card">
            <div className="card-header">
              <div className="card-title">{tab === 'promoters' ? 'Promoter Roster' : 'All Platform Users'}</div>
              <input className="input" style={{ width: 220 }} placeholder="Search…" value={search} onChange={e => setSearch(e.target.value)} />
            </div>
            <div style={{ padding: '0 18px' }}>
              {loading ? (
                <div className="page-loader"><div className="spinner" /></div>
              ) : tab === 'promoters' ? (
                filteredPromoters.length === 0 ? <div className="empty-state">No promoters found.</div> :
                filteredPromoters.map(p => (
                  <div className="person-row" key={p.id}>
                    <div className="avatar">{initials(p.firstName, p.lastName)}</div>
                    <div className="person-info">
                      <div className="person-name">{p.firstName} {p.lastName}</div>
                      <div className="person-sub">{p.email}</div>
                    </div>
                    <span className={`badge ${p.status === 'ACTIVE' ? 'badge-green' : 'badge-red'}`}>{p.status || 'ACTIVE'}</span>
                    <div className="person-actions">
                      <button className="btn btn-secondary btn-sm" onClick={() => startEdit(p)}>Edit</button>
                      <button className="btn btn-danger btn-sm" onClick={() => onDeactivate(p.id)}>Deactivate</button>
                      <button className="btn btn-sm" style={{ background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.4)', color: '#ef4444', fontSize: 11 }} onClick={() => onHardDeletePromoter(p.id, p.email)}>🗑 Delete</button>
                    </div>
                  </div>
                ))
              ) : (
                filteredUsers.length === 0 ? <div className="empty-state">No users found.</div> :
                filteredUsers.map(u => (
                  <div className="person-row" key={u.id}>
                    <div className="avatar">{initials(u.firstName, u.lastName)}</div>
                    <div className="person-info">
                      <div className="person-name">{u.firstName} {u.lastName}</div>
                      <div className="person-sub">{u.email}</div>
                    </div>
                    <span className="badge badge-gray" style={{ fontSize: 10 }}>{u.role}</span>
                    <span className={`badge ${u.status === 'ACTIVE' ? 'badge-green' : 'badge-red'}`}>{u.status}</span>
                    <div className="person-actions">
                      <button className="btn btn-sm" style={{ background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.4)', color: '#ef4444', fontSize: 11 }} onClick={() => onHardDeleteUser(u.id, u.email)}>🗑 Delete</button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
