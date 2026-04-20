import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { concertApi, extractErrorMessage } from '../api';
import { formatCurrency, formatDate, formatPercent } from '../utils/format';
import { normalizeConcertStatus, statusTone } from '../utils/app';
import Sidebar from '../components/common/Sidebar';

function statusLabel(concert) {
  const s = normalizeConcertStatus(concert.status);
  if (s === 'PUBLISHED') {
    const sold = concert.ticketsSold || 0, cap = concert.totalCapacity || 0;
    if (cap > 0 && sold >= cap) return { label: 'Sold Out', cls: 'badge-red' };
    if (cap > 0 && sold >= cap * 0.8) return { label: 'Selling Fast', cls: 'badge-yellow' };
    return { label: 'Active', cls: 'badge-green' };
  }
  if (s === 'DRAFT' || s === 'PENDING') return { label: 'Upcoming', cls: 'badge-purple' };
  return { label: s, cls: statusTone(s) };
}

export default function OrganizerConcertsPage() {
  const [concerts, setConcerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterOpen, setFilterOpen] = useState(false);
  const [filters, setFilters] = useState({ status: '', search: '' });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await concertApi.getMyConcerts({ page: 0, size: 50, sort: 'dateTime,desc' });
      setConcerts(data.content || []);
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Unable to load concerts'));
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const publish = async (id) => {
    try { await concertApi.publish(id); toast.success('Concert published!'); load(); }
    catch (err) { toast.error(extractErrorMessage(err, 'Could not publish')); }
  };

  const remove = async (id) => {
    if (!window.confirm('Remove this concert?')) return;
    try { await concertApi.delete(id); toast.success('Concert removed'); load(); }
    catch (err) { toast.error(extractErrorMessage(err, 'Could not remove')); }
  };

  const filtered = useMemo(() => concerts.filter(c => {
    const s = normalizeConcertStatus(c.status);
    if (filters.status && s !== filters.status) return false;
    if (filters.search) {
      const q = filters.search.toLowerCase();
      if (!`${c.title} ${c.genre || ''} ${c.venue?.city || ''}`.toLowerCase().includes(q)) return false;
    }
    return true;
  }), [concerts, filters]);

  const setF = k => e => setFilters(f => ({ ...f, [k]: e.target.value }));

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-content">
        <div className="page-wrap">
          <div className="page-header">
            <div>
              <div className="page-title">My Concerts</div>
              <div className="page-subtitle">{concerts.length} total · {concerts.filter(c => normalizeConcertStatus(c.status) === 'PUBLISHED').length} published</div>
            </div>
            <div className="page-actions">
              <button className="btn btn-secondary" onClick={() => setFilterOpen(o => !o)}>🔧 Filter</button>
              <Link to="/organizer/concerts/create" className="btn btn-primary">+ New Concert</Link>
            </div>
          </div>

          {/* Inline filter bar */}
          {filterOpen && (
            <div className="card card-p mb-16" style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
              <div style={{ flex: 1, minWidth: 160 }}>
                <label className="form-label">Search</label>
                <input className="input" placeholder="Name, genre, city…" value={filters.search} onChange={setF('search')} />
              </div>
              <div style={{ minWidth: 160 }}>
                <label className="form-label">Status</label>
                <select className="input select" value={filters.status} onChange={setF('status')}>
                  <option value="">All statuses</option>
                  <option value="PUBLISHED">Published</option>
                  <option value="DRAFT">Upcoming (Draft)</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
              <button className="btn btn-ghost btn-sm" onClick={() => setFilters({ status: '', search: '' })}>Clear</button>
            </div>
          )}

          {loading ? (
            <div className="page-loader"><div className="spinner" /></div>
          ) : filtered.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">🎪</div>
              <div style={{ marginBottom: 12 }}>No concerts yet. Create your first one!</div>
              <Link to="/organizer/concerts/create" className="btn btn-primary">Create Concert</Link>
            </div>
          ) : (
            <div>
              {filtered.map(concert => {
                const { label, cls } = statusLabel(concert);
                const sold = concert.ticketsSold || 0;
                const cap = concert.totalCapacity || 0;
                const pct = cap > 0 ? (sold / cap) * 100 : 0;
                const isUpcoming = normalizeConcertStatus(concert.status) === 'DRAFT' || normalizeConcertStatus(concert.status) === 'PENDING';

                return (
                  <div key={concert.id} style={{ background: 'var(--bg2)', border: '1px solid var(--border)', borderRadius: 'var(--r)', padding: '16px 18px', marginBottom: 10 }}>
                    <div className="concert-ops-row" style={{ border: 'none', padding: 0 }}>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div className="flex mb-8" style={{ gap: 6, flexWrap: 'wrap' }}>
                          <span className={`badge badge-dot ${cls}`}>{label}</span>
                          {concert.genre && <span className="badge badge-gray">{concert.genre}</span>}
                          {concert.venue?.city && <span className="badge badge-gray">📍 {concert.venue.city}</span>}
                        </div>
                        <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 3 }}>{concert.title}</div>
                        <div style={{ fontSize: 12.5, color: 'var(--muted)' }}>
                          {formatDate(concert.dateTime)}
                          {concert.venue?.address && ` · ${concert.venue.address}`}
                        </div>

                        {cap > 0 && (
                          <>
                            <div className="sales-bar" style={{ marginTop: 10 }}>
                              <div className="sales-bar-fill" style={{ width: `${Math.min(pct, 100)}%` }} />
                            </div>
                            <div style={{ fontSize: 11.5, color: 'var(--muted2)' }}>
                              {sold} / {cap} seats · {formatPercent(pct)} sold
                              {concert.ticketTypes?.length > 0 && ` · ${concert.ticketTypes.length} ticket type${concert.ticketTypes.length !== 1 ? 's' : ''}`}
                            </div>
                          </>
                        )}
                      </div>

                      <div className="concert-ops-actions">
                        <Link to={`/concerts/${concert.id}`} className="btn btn-ghost btn-sm">View</Link>
                        <Link to={`/organizer/concerts/${concert.id}/edit`} className="btn btn-secondary btn-sm">Edit</Link>
                        {isUpcoming && (
                          <button className="btn btn-primary btn-sm" onClick={() => publish(concert.id)}>Publish</button>
                        )}
                        <button className="btn btn-danger btn-sm" onClick={() => remove(concert.id)}>Delete</button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
