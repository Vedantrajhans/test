import React, { useEffect, useMemo, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { extractErrorMessage, promoterApi } from '../api';
import { downloadBlob, organizerTypes } from '../utils/app';
import { initials } from '../utils/format';
import Sidebar from '../components/common/Sidebar';

const emptyForm = { email: '', firstName: '', lastName: '', organizerType: organizerTypes[0], companyName: '', city: '', state: '', status: 'ACTIVE' };

export default function PromoterDashboard() {
  const [organizers, setOrganizers] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [query, setQuery] = useState('');
  const [cityFilter, setCityFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [importSummary, setImportSummary] = useState(null);
  const [showForm, setShowForm] = useState(false);
  // Fix #13: export filter state
  const [exportFilters, setExportFilters] = useState({ city: '', state: '', organizerType: '', search: '' });
  const [showExportFilters, setShowExportFilters] = useState(false);
  const fileRef = useRef(null);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await promoterApi.listOrganizers();
      setOrganizers(data || []);
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Unable to load organizers'));
    } finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => organizers.filter(o => {
    const hay = `${o.firstName} ${o.lastName} ${o.email} ${o.companyName || ''} ${o.city || ''}`.toLowerCase();
    return hay.includes(query.toLowerCase()) && (!cityFilter || o.city?.toLowerCase() === cityFilter.toLowerCase());
  }), [organizers, query, cityFilter]);

  const resetForm = () => { setForm(emptyForm); setEditingId(null); setShowForm(false); };

  const startEdit = (o) => {
    setEditingId(o.id);
    setForm({ email: o.email, firstName: o.firstName, lastName: o.lastName, organizerType: o.organizerType || organizerTypes[0], companyName: o.companyName || '', city: o.city || '', state: o.state || '', status: o.status || 'ACTIVE' });
    setShowForm(true);
    window.scrollTo(0, 0);
  };

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));
  const setEF = k => e => setExportFilters(f => ({ ...f, [k]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault(); setSaving(true);
    try {
      if (editingId) {
        const { data } = await promoterApi.updateOrganizer(editingId, { firstName: form.firstName, lastName: form.lastName, organizerType: form.organizerType, companyName: form.companyName, city: form.city, state: form.state, status: form.status });
        setOrganizers(curr => curr.map(o => o.id === editingId ? data : o));
        toast.success('Organizer updated');
      } else {
        const { data } = await promoterApi.createOrganizer({ email: form.email, firstName: form.firstName, lastName: form.lastName, organizerType: form.organizerType, companyName: form.companyName, city: form.city, state: form.state });
        setOrganizers(curr => [data, ...curr]);
        toast.success('Organizer created');
      }
      resetForm();
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Save failed'));
    } finally { setSaving(false); }
  };

  const onDeactivate = async (id) => {
    if (!window.confirm('Deactivate this organizer?')) return;
    try {
      await promoterApi.deactivateOrganizer(id);
      setOrganizers(curr => curr.map(o => o.id === id ? { ...o, status: 'INACTIVE' } : o));
      toast.success('Organizer deactivated');
    } catch (err) { toast.error(extractErrorMessage(err, 'Failed')); }
  };

  // Fix #2: Hard delete organizer
  const onHardDelete = async (id, email) => {
    if (!window.confirm(`PERMANENTLY DELETE organizer "${email}"? This cannot be undone.`)) return;
    try {
      await promoterApi.hardDeleteOrganizer(id);
      setOrganizers(curr => curr.filter(o => o.id !== id));
      toast.success('Organizer permanently deleted');
    } catch (err) { toast.error(extractErrorMessage(err, 'Failed')); }
  };

  const onImport = async (e) => {
    const file = e.target.files?.[0]; if (!file) return;
    try {
      const { data } = await promoterApi.importOrganizers(file);
      setImportSummary(data);
      toast.success('CSV imported');
      load();
    } catch (err) { toast.error(extractErrorMessage(err, 'Import failed')); }
    finally { e.target.value = ''; }
  };

  // Fix #13: flexible export with filters
  const onExport = async () => {
    try {
      const params = {};
      if (exportFilters.city) params.city = exportFilters.city;
      if (exportFilters.state) params.state = exportFilters.state;
      if (exportFilters.organizerType) params.organizerType = exportFilters.organizerType;
      if (exportFilters.search) params.search = exportFilters.search;
      const { data } = await promoterApi.exportOrganizers(params);
      downloadBlob(new Blob([data], { type: 'text/csv' }), 'organizers.csv');
      toast.success('CSV exported');
      setShowExportFilters(false);
    } catch (err) { toast.error(extractErrorMessage(err, 'Export failed')); }
  };

  const cities = [...new Set(organizers.map(o => o.city).filter(Boolean))];

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-content">
        <div className="page-wrap">
          <div className="page-header">
            <div>
              <div className="page-title">Promoter Dashboard</div>
              <div className="page-subtitle">Manage organizers</div>
            </div>
            <div className="page-actions">
              <button className="btn btn-secondary btn-sm" onClick={() => fileRef.current?.click()}>📥 Import CSV</button>
              <button className="btn btn-secondary btn-sm" onClick={() => setShowExportFilters(v => !v)}>📤 Export CSV</button>
              <input ref={fileRef} type="file" accept=".csv" hidden onChange={onImport} />
              <button className="btn btn-primary" onClick={() => { setEditingId(null); setForm(emptyForm); setShowForm(true); }}>+ Add Organizer</button>
            </div>
          </div>

          {/* Fix #13: flexible export filters panel */}
          {showExportFilters && (
            <div className="card card-p mb-16" style={{ maxWidth: 600 }}>
              <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 12 }}>Export CSV — Filters (leave blank to export all)</div>
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">City</label>
                  <input className="input" placeholder="Filter by city…" value={exportFilters.city} onChange={setEF('city')} />
                </div>
                <div className="form-group">
                  <label className="form-label">State</label>
                  <input className="input" placeholder="Filter by state…" value={exportFilters.state} onChange={setEF('state')} />
                </div>
                <div className="form-group">
                  <label className="form-label">Organizer Type</label>
                  <select className="input select" value={exportFilters.organizerType} onChange={setEF('organizerType')}>
                    <option value="">All types</option>
                    {organizerTypes.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Search (name/email)</label>
                  <input className="input" placeholder="Search…" value={exportFilters.search} onChange={setEF('search')} />
                </div>
              </div>
              <div className="flex" style={{ gap: 8 }}>
                <button className="btn btn-primary" onClick={onExport}>Download CSV</button>
                <button className="btn btn-secondary" onClick={() => { setExportFilters({ city: '', state: '', organizerType: '', search: '' }); setShowExportFilters(false); }}>Cancel</button>
              </div>
            </div>
          )}

          {/* Stats */}
          <div className="stat-row">
            <div className="stat-card"><div className="stat-label">Total Organizers</div><div className="stat-value">{organizers.length}</div></div>
            <div className="stat-card"><div className="stat-label">Active</div><div className="stat-value" style={{ color: 'var(--green)' }}>{organizers.filter(o => o.status === 'ACTIVE').length}</div></div>
            <div className="stat-card"><div className="stat-label">Inactive</div><div className="stat-value" style={{ color: 'var(--red)' }}>{organizers.filter(o => o.status !== 'ACTIVE').length}</div></div>
          </div>

          {importSummary && (
            <div className="banner banner-success mb-16">
              ✓ Import done: {importSummary.createdCount || 0} created, {importSummary.updatedCount || 0} updated.
              {importSummary.rowErrors?.length > 0 && <span style={{ color: 'var(--yellow)' }}> {importSummary.rowErrors.length} error(s).</span>}
              <button className="btn btn-ghost btn-sm" style={{ marginLeft: 'auto' }} onClick={() => setImportSummary(null)}>✕</button>
            </div>
          )}

          {/* ADD / EDIT FORM */}
          {showForm && (
            <div className="card card-p mb-16">
              <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>{editingId ? 'Edit Organizer' : 'Add Organizer'}</div>
              <form onSubmit={onSubmit}>
                <div className="grid-2">
                  <div className="form-group"><label className="form-label">First name</label><input className="input" value={form.firstName} onChange={set('firstName')} required /></div>
                  <div className="form-group"><label className="form-label">Last name</label><input className="input" value={form.lastName} onChange={set('lastName')} required /></div>
                </div>
                <div className="form-group"><label className="form-label">Email</label><input className="input" type="email" value={form.email} disabled={Boolean(editingId)} onChange={set('email')} required /></div>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label">Organizer type</label>
                    <select className="input select" value={form.organizerType} onChange={set('organizerType')}>
                      {organizerTypes.map(t => <option key={t} value={t}>{t}</option>)}
                    </select>
                  </div>
                  <div className="form-group"><label className="form-label">Company</label><input className="input" value={form.companyName} onChange={set('companyName')} placeholder="Company name" /></div>
                </div>
                <div className="grid-2">
                  <div className="form-group"><label className="form-label">City</label><input className="input" value={form.city} onChange={set('city')} placeholder="City" /></div>
                  <div className="form-group"><label className="form-label">State</label><input className="input" value={form.state} onChange={set('state')} placeholder="State" /></div>
                </div>
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
                  <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? <span className="spinner" /> : editingId ? 'Save changes' : 'Create organizer'}</button>
                </div>
              </form>
            </div>
          )}

          {/* FILTER + LIST */}
          <div className="card">
            <div className="card-header">
              <div className="card-title">Organizer List</div>
              <div className="flex" style={{ gap: 8 }}>
                <input className="input" style={{ width: 180 }} placeholder="Search…" value={query} onChange={e => setQuery(e.target.value)} />
                <select className="input select" style={{ width: 150 }} value={cityFilter} onChange={e => setCityFilter(e.target.value)}>
                  <option value="">All cities</option>
                  {cities.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
            </div>
            <div style={{ padding: '0 18px' }}>
              {loading ? (
                <div className="page-loader"><div className="spinner" /></div>
              ) : filtered.length === 0 ? (
                <div className="empty-state">No organizers found.</div>
              ) : (
                filtered.map(o => (
                  <div className="person-row" key={o.id}>
                    <div className="avatar">{initials(o.firstName, o.lastName)}</div>
                    <div className="person-info">
                      <div className="person-name">{o.firstName} {o.lastName}</div>
                      <div className="person-sub">{o.email}{o.city ? ` · ${o.city}` : ''}{o.companyName ? ` · ${o.companyName}` : ''}</div>
                      <div style={{ marginTop: 3, fontSize: 11, color: 'var(--muted2)' }}>{o.organizerType}</div>
                    </div>
                    <span className={`badge ${o.status === 'ACTIVE' ? 'badge-green' : 'badge-red'}`}>{o.status || 'ACTIVE'}</span>
                    <div className="person-actions">
                      <button className="btn btn-secondary btn-sm" onClick={() => startEdit(o)}>Edit</button>
                      <button className="btn btn-danger btn-sm" onClick={() => onDeactivate(o.id)}>Deactivate</button>
                      <button className="btn btn-sm" style={{ background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.4)', color: '#ef4444', fontSize: 11 }} onClick={() => onHardDelete(o.id, o.email)}>🗑 Delete</button>
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
