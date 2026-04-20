import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { concertApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { formatDate, formatCurrency } from '../utils/format';
import { normalizeConcertStatus, statusTone, getRoleHome } from '../utils/app';
import Navbar from '../components/common/Navbar';
import Footer from '../components/common/Footer';

function ConcertCard({ concert }) {
  const status = normalizeConcertStatus(concert.status);
  const badgeClass = statusTone(status);
  const label = status === 'PUBLISHED'
    ? concert.ticketsSold >= concert.totalCapacity ? 'Sold Out'
      : concert.ticketsSold >= concert.totalCapacity * 0.8 ? 'Selling Fast'
      : 'Active'
    : status === 'PENDING' || status === 'DRAFT' ? 'Upcoming'
    : status;

  const minPrice = concert.ticketTypes?.length
    ? Math.min(...concert.ticketTypes.map(t => t.price))
    : null;

  return (
    <Link to={`/concerts/${concert.id}`} className="concert-card">
      {concert.imageUrl ? (
        <div className="concert-card-img">
          <img src={concert.imageUrl} alt={concert.title} />
          <div className="concert-card-img-overlay" />
          <div className="concert-status-pos">
            <span className={`badge badge-dot ${badgeClass}`}>{label}</span>
          </div>
        </div>
      ) : (
        <div className="concert-no-img">
          🎸
          <div className="concert-status-pos">
            <span className={`badge badge-dot ${badgeClass}`}>{label}</span>
          </div>
        </div>
      )}
      <div className="concert-card-body">
        <div className="concert-card-title">{concert.title}</div>
        <div className="concert-card-meta">
          <span>📅 {formatDate(concert.dateTime)}</span>
          {concert.venue?.city && <span>📍 {concert.venue.city}</span>}
          {concert.genre && <span>🎵 {concert.genre}</span>}
        </div>
        {minPrice != null && (
          <div className="concert-card-price">
            <span>From {formatCurrency(minPrice)}</span>
            <span className={`badge ${badgeClass}`}>{label}</span>
          </div>
        )}
      </div>
    </Link>
  );
}

export default function HomePage() {
  const [concerts, setConcerts] = useState([]);
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    concertApi.search({ page: 0, size: 6, status: 'PUBLISHED', sort: 'dateTime,asc' })
      .then(r => setConcerts(r.data.content || []))
      .catch(() => {});
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />

      {/* HERO */}
      <section className="hero">
        <div className="hero-kicker">🎶 India's Concert Platform</div>
        <h1 className="hero-title">
          Live music.<br />
          Unforgettable moments.
        </h1>
        <p className="hero-sub">
          Discover concerts near you, book tickets in seconds, and experience the best live performances across India.
        </p>
        <div className="hero-actions">
          <Link to="/concerts" className="btn btn-primary btn-lg">Browse Concerts</Link>
          {isAuthenticated ? (
            <Link to={getRoleHome(user?.role)} className="btn btn-secondary btn-lg">Go to Dashboard</Link>
          ) : (
            <Link to="/register" className="btn btn-secondary btn-lg">Join Free</Link>
          )}
        </div>
      </section>

      {/* FEATURED CONCERTS */}
      {concerts.length > 0 && (
        <section style={{ padding: '0 32px 56px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
          <div className="flex-between mb-20">
            <div>
              <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--accent2)', marginBottom: 6 }}>
                Upcoming Shows
              </div>
              <div style={{ fontFamily: "'Syne', sans-serif", fontSize: '1.4rem', fontWeight: 800 }}>Featured Concerts</div>
            </div>
            <Link to="/concerts" className="btn btn-ghost btn-sm">View all →</Link>
          </div>
          <div className="concerts-grid">
            {concerts.map(c => <ConcertCard key={c.id} concert={c} />)}
          </div>
        </section>
      )}

      {/* HOW IT WORKS */}
      <section style={{ background: 'var(--bg2)', borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)', padding: '52px 32px' }}>
        <div style={{ maxWidth: 1200, margin: '0 auto' }}>
          <div className="text-center mb-24">
            <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--accent2)', marginBottom: 8 }}>Simple Process</div>
            <div style={{ fontFamily: "'Syne', sans-serif", fontSize: '1.4rem', fontWeight: 800 }}>How Festiva Works</div>
          </div>
          <div className="grid-3" style={{ gap: 20 }}>
            {[
              { icon: '🔍', title: 'Discover', desc: 'Search and filter concerts by city, date, genre, and more.' },
              { icon: '🎫', title: 'Book', desc: 'Choose your ticket type, confirm your seats in one click.' },
              { icon: '🎉', title: 'Experience', desc: 'Scan your QR ticket at the gate and enjoy the show!' },
            ].map(item => (
              <div key={item.title} className="card card-p" style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '2rem', marginBottom: 12 }}>{item.icon}</div>
                <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 6 }}>{item.title}</div>
                <div style={{ fontSize: 13, color: 'var(--muted)', lineHeight: 1.65 }}>{item.desc}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ROLE CARDS */}
      <section style={{ padding: '52px 32px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <div className="text-center mb-24">
          <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--accent2)', marginBottom: 8 }}>For Everyone</div>
          <div style={{ fontFamily: "'Syne', sans-serif", fontSize: '1.4rem', fontWeight: 800 }}>Built for the entire ecosystem</div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14 }}>
          {[
            { role: 'Attendee', icon: '🎟️', desc: 'Discover shows, book tickets, and collect memories.' },
            { role: 'Organizer', icon: '🎪', desc: 'Create concerts, manage capacity, and track revenue.' },
            { role: 'Promoter', icon: '📊', desc: 'Onboard organizers and manage your event network.' },
            { role: 'Producer', icon: '🏭', desc: 'Oversee the entire platform from the top.' },
          ].map(item => (
            <div key={item.role} className="card card-p">
              <div style={{ fontSize: '1.6rem', marginBottom: 10 }}>{item.icon}</div>
              <div style={{ fontWeight: 700, marginBottom: 5 }}>{item.role}</div>
              <div style={{ fontSize: 12.5, color: 'var(--muted)', lineHeight: 1.6 }}>{item.desc}</div>
            </div>
          ))}
        </div>
      </section>

      <Footer />
    </div>
  );
}
