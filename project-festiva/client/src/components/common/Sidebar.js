import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../../context/AuthContext';

/* ── tiny inline SVG icons ── */
function Ic({ path, size = 15 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d={path} />
    </svg>
  );
}

const PATHS = {
  home:      'M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
  concerts:  'M9 19V6l12-3v13M9 19c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2zm12-3c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2z',
  ticket:    'M15 5v2m0 4v2m0 4v2M5 5a2 2 0 0 0-2 2v3a2 2 0 0 1 0 4v3a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-3a2 2 0 0 1 0-4V7a2 2 0 0 0-2-2H5z',
  user:      'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z',
  chart:     'M18 20V10M12 20V4M6 20v-6',
  logout:    'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  csv:       'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M16 13H8M16 17H8M10 9H8',
  plus:      'M12 5v14M5 12h14',
};

const NAV = {
  ATTENDEE: [
    { to: '/',            label: 'Home',        icon: 'home' },
    { to: '/concerts',    label: 'Concerts',    icon: 'concerts' },
    { to: '/my-bookings', label: 'My Tickets',  icon: 'ticket' },
    { to: '/profile',     label: 'Profile',     icon: 'user' },
  ],
  ORGANIZER: [
    { to: '/organizer/concerts',  label: 'Concerts',    icon: 'concerts' },
    { to: '/organizer/analytics', label: 'Analytics',   icon: 'chart' },
    { to: '/organizer/profile',   label: 'Profile',     icon: 'user' },
  ],
  PROMOTER: [
    { to: '/promoter',         label: 'Analytics',         icon: 'chart' },
    { to: '/promoter/csv',     label: 'Import / Export',   icon: 'csv' },
    { to: '/promoter/profile', label: 'Profile',           icon: 'user' },
  ],
  PRODUCER: [
    { to: '/producer',         label: 'Analytics',   icon: 'chart' },
    { to: '/producer/profile', label: 'Profile',     icon: 'user' },
  ],
};

function initials(email) {
  return (email || '??').slice(0, 2).toUpperCase();
}

export default function Sidebar() {
  const { user, logout, isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  if (!isAuthenticated) return null;

  const links = NAV[user?.role] || [];

  const isActive = (to) => {
    if (to === '/') return location.pathname === '/';
    return location.pathname === to || location.pathname.startsWith(to + '/');
  };

  const handleLogout = () => {
    logout();
    toast.success('Signed out');
    navigate('/');
  };

  const roleLabel = { ATTENDEE: 'Attendee', ORGANIZER: 'Organizer', PROMOTER: 'Promoter', PRODUCER: 'Producer' };

  return (
    <aside className="sidebar">
      {/* Brand */}
      <div className="sidebar-brand">
        <Link to="/" className="brand-logo">
          <div className="brand-icon">🎵</div>
          <span className="brand-text">Festiva</span>
        </Link>
      </div>

      {/* Nav links */}
      <nav className="sidebar-nav">
        <div className="nav-label">Menu</div>
        {links.map(link => (
          <Link
            key={link.to}
            to={link.to}
            className={`sidebar-link ${isActive(link.to) ? 'active' : ''}`}
          >
            <Ic path={PATHS[link.icon]} />
            {link.label}
          </Link>
        ))}
      </nav>

      {/* User + logout */}
      <div className="sidebar-footer">
        <div className="sidebar-user">
          <div className="user-av">{initials(user?.email)}</div>
          <div className="user-info">
            <div className="user-name">{user?.email || 'User'}</div>
            <div className="user-role-text">{roleLabel[user?.role] || user?.role}</div>
          </div>
        </div>
        <button
          className="sidebar-link"
          style={{ width: '100%', background: 'none', border: 'none', cursor: 'pointer', textAlign: 'left' }}
          onClick={handleLogout}
        >
          <Ic path={PATHS.logout} />
          Logout
        </button>
      </div>
    </aside>
  );
}
