import React, { useCallback, useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { concertApi, extractErrorMessage, feedbackApi } from '../api';
import { formatCurrency, formatDate, formatPercent } from '../utils/format';
import { downloadBlob, normalizeConcertStatus, statusTone } from '../utils/app';
import Sidebar from '../components/common/Sidebar';

function Stars({ v }) {
  return (
    <span>{[1,2,3,4,5].map(i => <span key={i} style={{ fontSize: 12, color: i <= v ? 'var(--yellow)' : 'rgba(255,255,255,0.12)' }}>★</span>)}</span>
  );
}

function BookingWindowBadge({ concert }) {
  const now = new Date();
  const saleStart = concert.ticketSaleStart ? new Date(concert.ticketSaleStart) : null;
  const saleEnd = concert.ticketSaleEnd ? new Date(concert.ticketSaleEnd) : null;
  const concertDate = concert.dateTime ? new Date(concert.dateTime) : null;

  if (saleEnd && now > saleEnd) return <span className="badge badge-red">Booking Closed</span>;
  if (saleStart && now < saleStart) return <span className="badge badge-purple">Booking Not Open</span>;
  if (concertDate && now > concertDate) return <span className="badge badge-gray">Past</span>;
  return <span className="badge badge-green">Booking Open</span>;
}

export default function OrganizerAnalyticsPage() {
  const [concerts, setConcerts] = useState([]);
  const [feedback, setFeedback] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [bookingFilter, setBookingFilter] = useState(''); // 'open' | 'closed' | ''

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await concertApi.getMyConcerts({ page: 0, size: 50 });
      const items = data.content || [];
      setConcerts(items);
      if (!selected && items[0]?.id) setSelected(items[0].id);
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Unable to load'));
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!selected) return;
    feedbackApi.getForConcert(selected)
      .then(({ data }) => setFeedback(data || []))
      .catch(() => setFeedback([]));
  }, [selected]);

  const totals = useMemo(() => ({
    total: concerts.length,
    published: concerts.filter(c => normalizeConcertStatus(c.status) === 'PUBLISHED').length,
    sold: concerts.reduce((s, c) => s + (c.ticketsSold || 0), 0),
    cap: concerts.reduce((s, c) => s + (c.totalCapacity || 0), 0),
  }), [concerts]);

  // Fix #7: booking window filter
  const isBookingOpen = (c) => {
    const now = new Date();
    const saleStart = c.ticketSaleStart ? new Date(c.ticketSaleStart) : null;
    const saleEnd = c.ticketSaleEnd ? new Date(c.ticketSaleEnd) : null;
    const afterStart = !saleStart || now >= saleStart;
    const beforeEnd = !saleEnd || now <= saleEnd;
    return afterStart && beforeEnd;
  };

  const filteredConcerts = useMemo(() => {
    let list = concerts;
    if (statusFilter) list = list.filter(c => normalizeConcertStatus(c.status) === statusFilter);
    if (bookingFilter === 'open') list = list.filter(c => isBookingOpen(c));
    if (bookingFilter === 'closed') list = list.filter(c => !isBookingOpen(c));
    // Fix #7: open booking concerts at top, closed at end
    list = [...list].sort((a, b) => {
      const ao = isBookingOpen(a), bo = isBookingOpen(b);
      if (ao && !bo) return -1;
      if (!ao && bo) return 1;
      return 0;
    });
    return list;
  }, [concerts, statusFilter, bookingFilter]);

  const focusConcert = concerts.find(c => c.id === selected);
  const avgRating = feedback.length ? (feedback.reduce((s, f) => s + f.rating, 0) / feedback.length).toFixed(1) : null;

  const exportAttendees = async (concertId) => {
    try {
      const { data } = await concertApi.exportAttendees(concertId);
      downloadBlob(new Blob([data], { type: 'text/csv' }), 'attendees.csv');
      toast.success('Attendee CSV exported');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Export failed (only for concerts with capacity < 500)'));
    }
  };

  if (loading) return (
    <div className="app-shell"><Sidebar /><div className="main-content"><div className="page-loader"><div className="spinner" /></div></div></div>
  );

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-content">
        <div className="page-wrap">
          <div className="page-header">
            <div>
              <div className="page-title">Analytics</div>
              <div className="page-subtitle">Concert dashboard overview</div>
            </div>
            {/* Fix #7: booking window filter */}
            <div className="flex" style={{ gap: 8 }}>
              <select className="input select" style={{ minWidth: 150 }} value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                <option value="">All statuses</option>
                <option value="PUBLISHED">Published</option>
                <option value="DRAFT">Draft / Upcoming</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
              <select className="input select" style={{ minWidth: 160 }} value={bookingFilter} onChange={e => setBookingFilter(e.target.value)}>
                <option value="">All booking windows</option>
                <option value="open">Booking Open</option>
                <option value="closed">Booking Closed</option>
              </select>
            </div>
          </div>

          {/* TOP STATS */}
          <div className="stat-row">
            <div className="stat-card"><div className="stat-label">Total Concerts</div><div className="stat-value">{totals.total}</div></div>
            <div className="stat-card"><div className="stat-label">Published</div><div className="stat-value" style={{ color: 'var(--green)' }}>{totals.published}</div></div>
            <div className="stat-card"><div className="stat-label">Tickets Sold</div><div className="stat-value">{totals.sold}</div></div>
            <div className="stat-card"><div className="stat-label">Total Capacity</div><div className="stat-value">{totals.cap}</div></div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 20, alignItems: 'start' }}>
            {/* CONCERT LIST */}
            <div className="card">
              <div className="card-header">
                <div><div className="card-title">All Concerts</div><div className="card-subtitle">Click to view analytics</div></div>
              </div>
              <div style={{ padding: '10px 18px' }}>
                {filteredConcerts.length === 0 && <div className="empty-state">No concerts match the filter.</div>}
                {filteredConcerts.map(c => {
                  const s = normalizeConcertStatus(c.status);
                  const sold = c.ticketsSold || 0, cap = c.totalCapacity || 0;
                  const pct = cap > 0 ? (sold / cap) * 100 : 0;
                  return (
                    <div key={c.id} onClick={() => setSelected(c.id)}
                      style={{ padding: '12px 0', borderBottom: '1px solid var(--border)', cursor: 'pointer', background: selected === c.id ? 'rgba(255,92,53,0.04)' : 'transparent', borderRadius: 6, paddingLeft: selected === c.id ? 8 : 0, transition: 'all 0.15s' }}>
                      <div className="flex-between mb-4">
                        <span style={{ fontWeight: 700, fontSize: 13.5 }}>{c.title}</span>
                        <div className="flex" style={{ gap: 4 }}>
                          <BookingWindowBadge concert={c} />
                          <span className={`badge ${statusTone(s)}`}>{s}</span>
                        </div>
                      </div>
                      <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 6 }}>{formatDate(c.dateTime)}{c.venue?.city ? ` · ${c.venue.city}` : ''}</div>
                      {cap > 0 && (
                        <>
                          <div className="sales-bar"><div className="sales-bar-fill" style={{ width: `${Math.min(pct, 100)}%` }} /></div>
                          <div style={{ fontSize: 11, color: 'var(--muted2)' }}>{sold}/{cap} · {formatPercent(pct)}</div>
                        </>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* FOCUS PANEL */}
            <div>
              {focusConcert ? (
                <>
                  <div className="card mb-14">
                    <div className="card-header">
                      <div>
                        <div className="card-title">{focusConcert.title}</div>
                        <div className="card-subtitle">{formatDate(focusConcert.dateTime)}</div>
                      </div>
                      {/* Fix #12: export attendees button (only for capacity < 500) */}
                      {focusConcert.totalCapacity < 500 && (
                        <button className="btn btn-secondary btn-sm" onClick={() => exportAttendees(focusConcert.id)}>
                          📋 Export Attendees
                        </button>
                      )}
                    </div>
                    <div style={{ padding: '14px 18px' }}>
                      <div className="stat-row" style={{ gridTemplateColumns: 'repeat(2, 1fr)', marginBottom: 14 }}>
                        <div className="stat-card"><div className="stat-label">Sold</div><div className="stat-value" style={{ fontSize: '1.3rem' }}>{focusConcert.ticketsSold || 0}</div></div>
                        <div className="stat-card"><div className="stat-label">Remaining</div><div className="stat-value" style={{ fontSize: '1.3rem' }}>{Math.max((focusConcert.totalCapacity || 0) - (focusConcert.ticketsSold || 0), 0)}</div></div>
                      </div>

                      {/* Fix #11: per ticket-type sold quantity */}
                      {focusConcert.ticketTypes?.length > 0 && (
                        <>
                          <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8 }}>Ticket Sales by Type</div>
                          {focusConcert.ticketTypes.map(tt => {
                            const total = (tt.availableQuantity || 0) + (tt.soldQuantity || 0);
                            const pct = total > 0 ? Math.round((tt.soldQuantity / total) * 100) : 0;
                            return (
                              <div key={tt.id} style={{ marginBottom: 12 }}>
                                <div className="flex-between" style={{ fontSize: 13, marginBottom: 4 }}>
                                  <span style={{ fontWeight: 600 }}>{tt.name}</span>
                                  <span style={{ color: 'var(--accent2)', fontWeight: 700 }}>{formatCurrency(tt.price)}</span>
                                </div>
                                <div className="sales-bar" style={{ marginBottom: 4 }}>
                                  <div className="sales-bar-fill" style={{ width: `${Math.min(pct, 100)}%`, background: tt.availableQuantity === 0 ? '#ef4444' : undefined }} />
                                </div>
                                <div className="flex-between" style={{ fontSize: 11, color: 'var(--muted2)' }}>
                                  <span>{tt.soldQuantity || 0} sold / {total} total</span>
                                  <span className={`badge ${tt.availableQuantity === 0 ? 'badge-red' : tt.availableQuantity < total * 0.2 ? 'badge-yellow' : 'badge-green'}`} style={{ fontSize: 10 }}>
                                    {tt.availableQuantity === 0 ? 'Sold Out' : tt.availableQuantity < total * 0.2 ? 'Selling Fast' : `${tt.availableQuantity} left`}
                                  </span>
                                </div>
                              </div>
                            );
                          })}
                        </>
                      )}
                    </div>
                  </div>

                  <div className="card">
                    <div className="card-header">
                      <div>
                        <div className="card-title">Reviews</div>
                        <div className="card-subtitle">{feedback.length} reviews{avgRating ? ` · avg ${avgRating} ★` : ''}</div>
                      </div>
                    </div>
                    <div style={{ padding: '12px 18px' }}>
                      {feedback.length === 0 ? (
                        <div className="empty-state" style={{ padding: '16px 0' }}>No reviews yet.</div>
                      ) : (
                        feedback.slice(0, 5).map(item => (
                          <div key={item.id} style={{ padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
                            <div className="flex-between mb-4">
                              <Stars v={item.rating} />
                              <span style={{ fontSize: 11, color: 'var(--muted2)' }}>{item.rating}/5</span>
                            </div>
                            {item.comment && <div style={{ fontSize: 13, color: 'var(--muted)', lineHeight: 1.5 }}>{item.comment}</div>}
                            <div style={{ fontSize: 11, color: 'var(--muted2)', marginTop: 4 }}>Sound {item.soundQuality} · Venue {item.venueExperience} · Perf {item.artistPerformance}</div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                </>
              ) : (
                <div className="card card-p"><div className="empty-state">Select a concert to view details.</div></div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
