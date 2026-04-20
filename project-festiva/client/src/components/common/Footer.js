import React from 'react';
import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer-inner">
        <div>
          <div className="footer-brand-name">
            <span style={{ fontSize: '18px' }}>🎵</span> Festiva
          </div>
          <p className="footer-desc">The all-in-one concert platform for producers, promoters, organizers, and fans.</p>
        </div>
        <div className="footer-col">
          <h4>Discover</h4>
          <Link to="/concerts">Browse Concerts</Link>
          <Link to="/register">Join as Attendee</Link>
          <Link to="/login">Sign In</Link>
        </div>
        <div className="footer-col">
          <h4>Platform</h4>
          <a href="#features">Features</a>
          <a href="#pricing">Pricing</a>
          <a href="#contact">Contact</a>
        </div>
        <div className="footer-col">
          <h4>Legal</h4>
          <a href="#privacy">Privacy Policy</a>
          <a href="#terms">Terms of Service</a>
          <a href="#cookies">Cookie Policy</a>
        </div>
      </div>
      <div className="footer-bottom">
        <span>© {new Date().getFullYear()} Festiva. All rights reserved.</span>
        <span>Made with ♥ for music lovers</span>
      </div>
    </footer>
  );
}
