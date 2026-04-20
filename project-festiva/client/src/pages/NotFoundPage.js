import React from 'react';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', textAlign: 'center', padding: 32 }}>
      <div>
        <div style={{ fontSize: '4rem', marginBottom: 16 }}>🎵</div>
        <div style={{ fontFamily: "'Syne', sans-serif", fontSize: '3rem', fontWeight: 800, marginBottom: 8 }}>404</div>
        <div style={{ fontSize: 16, color: 'var(--muted)', marginBottom: 24 }}>This page doesn't exist.</div>
        <Link to="/" className="btn btn-primary">Back to Home</Link>
      </div>
    </div>
  );
}
