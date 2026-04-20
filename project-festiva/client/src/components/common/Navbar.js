import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../../context/AuthContext';
import { getRoleHome } from '../../utils/app';

export default function Navbar({ searchValue, onSearchChange }) {
  const { user, isAuthenticated, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const fn = () => setScrolled(window.scrollY > 16);
    window.addEventListener('scroll', fn);
    return () => window.removeEventListener('scroll', fn);
  }, []);

  const handleLogout = () => {
    logout();
    toast.success('Signed out');
    navigate('/');
  };

  const isConcertsPage = location.pathname === '/concerts';

  return (
    <nav className={`topbar ${scrolled ? 'scrolled' : ''}`}>
      <div className="topbar-inner">
        <Link to="/" className="topbar-brand">
          <div className="topbar-brand-icon">🎵</div>
          Festiva
        </Link>

        {isConcertsPage && onSearchChange ? (
          <div className="topbar-search">
            <svg className="si" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
            <input placeholder="Search concerts, artists, cities…" value={searchValue || ''} onChange={(e) => onSearchChange(e.target.value)} />
          </div>
        ) : (
          <div className="topbar-links">
            <Link to="/" className={`topbar-link ${location.pathname === '/' ? 'active' : ''}`}>Home</Link>
            <Link to="/concerts" className={`topbar-link ${location.pathname.startsWith('/concerts') ? 'active' : ''}`}>Concerts</Link>
          </div>
        )}

        <div className="topbar-actions">
          {isAuthenticated ? (
            <>
              <Link to={getRoleHome(user?.role)} className="btn btn-secondary btn-sm">Dashboard</Link>
              <button className="btn btn-ghost btn-sm" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">Login</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Register</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
