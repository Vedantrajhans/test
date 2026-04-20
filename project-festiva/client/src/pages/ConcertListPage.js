import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { concertApi, extractErrorMessage } from '../api';
import { useAuth } from '../context/AuthContext';
import { getPreferredCity, normalizeConcertStatus, savePreferredCity, statusTone } from '../utils/app';
import { formatCurrency, formatDate, formatDateShort } from '../utils/format';
import Navbar from '../components/common/Navbar';
import Footer from '../components/common/Footer';
import Sidebar from '../components/common/Sidebar';

function isBookingOpen(c) {
  const now = new Date();
  const start = c.ticketSaleStart ? new Date(c.ticketSaleStart) : null;
  const end = c.ticketSaleEnd ? new Date(c.ticketSaleEnd) : null;
  return (!start || now >= start) && (!end || now <= end);
}

function statusLabel(concert) {
  const s = normalizeConcertStatus(concert.status);
  if (s === 'PUBLISHED') {
    const sold = concert.ticketsSold || 0;
    const cap = concert.totalCapacity || 0;
    if (sold >= cap) return { label: 'Sold Out', cls: 'badge-red' };
    if (cap > 0 && sold >= cap * 0.8) return { label: 'Selling Fast', cls: 'badge-yellow' };
    return { label: 'Active', cls: 'badge-green' };
  }
  if (s === 'DRAFT' || s === 'PENDING') return { label: 'Upcoming', cls: 'badge-purple' };
  return { label: s, cls: statusTone(s) };
}

function ConcertCard({ concert }) {
  const { label, cls } = statusLabel(concert);
  const minPrice = concert.ticketTypes?.length
    ? Math.min(...concert.ticketTypes.map(t => t.price)) : null;

  return (
    <Link to={`/concerts/${concert.id}`} className="concert-card">
      {concert.imageUrl ? (
        <div className="concert-card-img">
          <img src={concert.imageUrl} alt={concert.title} />
          <div className="concert-card-img-overlay" />
          <div className="concert-status-pos">
            <span className={`badge badge-dot ${cls}`}>{label}</span>
          </div>
        </div>
      ) : (
        <div className="concert-no-img">
          🎸
          <div className="concert-status-pos">
            <span className={`badge badge-dot ${cls}`}>{label}</span>
          </div>
        </div>
      )}
      <div className="concert-card-body">
        <div className="concert-card-title">{concert.title}</div>
        <div className="concert-card-meta">
          <div>📅 {formatDate(concert.dateTime)}</div>
          {concert.venue?.city && <div>📍 {concert.venue.city}</div>}
          {concert.genre && <div>🎵 {concert.genre}</div>}
        </div>
        {minPrice != null && (
          <div className="concert-card-price">
            <span>From {formatCurrency(minPrice)}</span>
          </div>
        )}
      </div>
    </Link>
  );
}

export default function ConcertListPage() {
  const { isAuthenticated, user } = useAuth();
  const [concerts, setConcerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterOpen, setFilterOpen] = useState(false);
  const [bookingWindowFilter, setBookingWindowFilter] = useState('');
  const [filters, setFilters] = useState({
    genre: '', dateFrom: '', dateTo: '', city: '', name: '',
    preferredCity: getPreferredCity(),
  });

  useEffect(() => {
    setLoading(true);
    concertApi.search({ page: 0, size: 100, sort: 'dateTime,asc' })
      .then(({ data }) => setConcerts(data.content || []))
      .catch(err => toast.error(extractErrorMessage(err, 'Unable to load concerts')))
      .finally(() => setLoading(false));
  }, []);

  const genres = [...new Set(concerts.map(c => c.genre).filter(Boolean))];
  const cities = [...new Set(concerts.map(c => c.venue?.city).filter(Boolean))];

  const filtered = useMemo(() => {
    const base = concerts.filter(c => {
      const haystack = `${c.title} ${c.description || ''} ${c.genre || ''} ${c.venue?.city || ''}`.toLowerCase();
      const q = (search || filters.name || '').toLowerCase();
      if (q && !haystack.includes(q)) return false;
      if (filters.genre && c.genre !== filters.genre) return false;
      if (filters.city && c.venue?.city?.toLowerCase() !== filters.city.toLowerCase()) return false;
      if (filters.dateFrom && new Date(c.dateTime) < new Date(filters.dateFrom)) return false;
      if (filters.dateTo && new Date(c.dateTime) > new Date(filters.dateTo)) return false;
      if (bookingWindowFilter === 'open' && !isBookingOpen(c)) return false;
      if (bookingWindowFilter === 'closed' && isBookingOpen(c)) return false;
      return true;
    });
    // Sort: open booking concerts first, closed at end
    return [...base].sort((a, b) => {
      const ao = isBookingOpen(a), bo = isBookingOpen(b);
      return ao === bo ? 0 : ao ? -1 : 1;
    });
  }, [concerts, search, filters, bookingWindowFilter]);

  const setF = k => e => setFilters(f => ({ ...f, [k]: e.target.value }));
  const clearFilters = () => setFilters({ genre: '', dateFrom: '', dateTo: '', city: '', name: '', preferredCity: '' });

  const isLoggedInAttendee = isAuthenticated && user?.role === 'ATTENDEE';

  // Decide layout: sidebar if logged in attendee, else public topbar
  if (isLoggedInAttendee) {
    return (
      <div className="app-shell">
        <Sidebar />
        <div className="main-content">
          <div className="page-wrap">
            <div className="page-header">
              <div>
                <div className="page-title">Concerts</div>
                <div className="page-subtitle">{filtered.length} shows available</div>
              </div>
              <div className="flex" style={{ gap: 8 }}>
                <select className="input select" style={{ minWidth: 160 }} value={bookingWindowFilter} onChange={e => setBookingWindowFilter(e.target.value)}>
                  <option value="">All concerts</option>
                  <option value="open">Booking Open</option>
                  <option value="closed">Booking Closed</option>
                </select>
                <button className="btn btn-secondary" onClick={() => setFilterOpen(true)}>
                  🔧 Filter
                </button>
              </div>
            </div>

            {/* Inline search */}
            <div style={{ position: 'relative', marginBottom: 20 }}>
              <svg style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)' }} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
              </svg>
              <input
                className="input"
                style={{ paddingLeft: 36 }}
                placeholder="Search concerts, genres, cities…"
                value={search}
                onChange={e => setSearch(e.target.value)}
              />
            </div>

            {loading ? (
              <div className="page-loader"><div className="spinner" /></div>
            ) : filtered.length === 0 ? (
              <div className="empty-state"><div className="empty-icon">🎵</div>No concerts found matching your filters.</div>
            ) : (
              <div className="concerts-grid">
                {filtered.map(c => <ConcertCard key={c.id} concert={c} />)}
              </div>
            )}
          </div>

          <FilterDrawer
            open={filterOpen}
            onClose={() => setFilterOpen(false)}
            filters={filters}
            setF={setF}
            genres={genres}
            cities={cities}
            onClear={clearFilters}
          />
        </div>
      </div>
    );
  }

  // Public layout
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar searchValue={search} onSearchChange={setSearch} />

      <div style={{ flex: 1, padding: '32px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <div className="page-header">
          <div>
            <div className="page-title">All Concerts</div>
            <div className="page-subtitle">{filtered.length} shows available</div>
          </div>
          <button className="btn btn-secondary" onClick={() => setFilterOpen(true)}>
            🔧 Filter
          </button>
        </div>

        {loading ? (
          <div className="page-loader"><div className="spinner" /></div>
        ) : filtered.length === 0 ? (
          <div className="empty-state"><div className="empty-icon">🎵</div>No concerts found.</div>
        ) : (
          <div className="concerts-grid">
            {filtered.map(c => <ConcertCard key={c.id} concert={c} />)}
          </div>
        )}
      </div>

      <FilterDrawer
        open={filterOpen}
        onClose={() => setFilterOpen(false)}
        filters={filters}
        setF={setF}
        genres={genres}
        cities={cities}
        onClear={clearFilters}
      />

      <Footer />
    </div>
  );
}

function FilterDrawer({ open, onClose, filters, setF, genres, cities, onClear }) {
  return (
    <>
      {open && <div className="filter-overlay" onClick={onClose} />}
      <div className={`filter-drawer ${open ? 'open' : ''}`}>
        <div className="filter-title">
          Filters
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>

        <div className="form-group">
          <label className="form-label">Concert name</label>
          <input className="input" placeholder="Search by name" value={filters.name} onChange={setF('name')} />
        </div>
        <div className="form-group">
          <label className="form-label">City</label>
          <select className="input select" value={filters.city} onChange={setF('city')}>
            <option value="">All cities</option>
            {cities.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        <div className="form-group">
          <label className="form-label">Genre</label>
          <select className="input select" value={filters.genre} onChange={setF('genre')}>
            <option value="">All genres</option>
            {genres.map(g => <option key={g} value={g}>{g}</option>)}
          </select>
        </div>
        <div className="form-group">
          <label className="form-label">From date</label>
          <input className="input" type="date" value={filters.dateFrom} onChange={setF('dateFrom')} />
        </div>
        <div className="form-group">
          <label className="form-label">To date</label>
          <input className="input" type="date" value={filters.dateTo} onChange={setF('dateTo')} />
        </div>
        <button className="btn btn-secondary w-full" style={{ justifyContent: 'center', marginTop: 8 }} onClick={onClear}>
          Clear all filters
        </button>
      </div>
    </>
  );
}
