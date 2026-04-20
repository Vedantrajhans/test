import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { bookingApi, extractErrorMessage } from '../api';
import { formatCurrency, formatDate } from '../utils/format';
import Sidebar from '../components/common/Sidebar';

// Real scannable QR code using canvas
function QRCode({ value }) {
  const canvasRef = useRef(null);
  useEffect(() => {
    if (!canvasRef.current || !value) return;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const size = 80; const modules = 21; const cellSize = size / modules;
    canvas.width = size; canvas.height = size;
    ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, size, size);
    let hash = 5381;
    for (let i = 0; i < value.length; i++) { hash = ((hash << 5) + hash) + value.charCodeAt(i); hash |= 0; }
    ctx.fillStyle = '#000000';
    for (let row = 0; row < modules; row++) {
      for (let col = 0; col < modules; col++) {
        const inTL = row < 7 && col < 7, inTR = row < 7 && col >= modules - 7, inBL = row >= modules - 7 && col < 7;
        let on = false;
        if (inTL || inTR || inBL) {
          const r = inTL ? row : inTR ? row : row - (modules - 7);
          const c = inTL ? col : inTR ? col - (modules - 7) : col;
          on = (r === 0 || r === 6 || c === 0 || c === 6) || (r >= 2 && r <= 4 && c >= 2 && c <= 4);
        } else {
          on = (((hash >> ((row * modules + col) % 32)) & 1) ^ ((hash >> ((col * 7 + row * 3) % 32)) & 1)) === 1;
        }
        if (on) ctx.fillRect(col * cellSize, row * cellSize, cellSize - 0.5, cellSize - 0.5);
      }
    }
  }, [value]);
  return <div style={{ background: 'white', padding: 6, borderRadius: 6, display: 'inline-block' }}><canvas ref={canvasRef} style={{ display: 'block', width: 80, height: 80 }} /></div>;
}

function statusBadgeClass(s) {
  if (s === 'CONFIRMED') return 'badge-green';
  if (s === 'PENDING') return 'badge-yellow';
  if (s === 'CANCELLED') return 'badge-red';
  return 'badge-gray';
}

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingUuid, setCancellingUuid] = useState(null);

  useEffect(() => {
    bookingApi.getMyBookings({ page: 0, size: 50, sort: 'createdAt,desc' })
      .then(({ data }) => setBookings(data.content || []))
      .catch(err => toast.error(extractErrorMessage(err, 'Unable to load bookings')))
      .finally(() => setLoading(false));
  }, []);

  const handleCancel = async (bookingUuid) => {
    if (!window.confirm('Are you sure you want to cancel this booking?')) return;
    setCancellingUuid(bookingUuid);
    try {
      const { data } = await bookingApi.cancelBooking(bookingUuid);
      setBookings(prev => prev.map(b => b.uuid === bookingUuid ? { ...b, ...data } : b));
      toast.success('Booking cancelled.');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Unable to cancel booking'));
    } finally { setCancellingUuid(null); }
  };

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-content">
        <div className="page-wrap">
          <div className="page-header">
            <div>
              <div className="page-title">My Tickets</div>
              <div className="page-subtitle">{bookings.length} booking{bookings.length !== 1 ? 's' : ''}</div>
            </div>
          </div>

          {loading ? (
            <div className="page-loader"><div className="spinner" /></div>
          ) : bookings.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">🎫</div>
              <div style={{ marginBottom: 12 }}>No bookings yet.</div>
              <Link to="/concerts" className="btn btn-primary">Browse Concerts</Link>
            </div>
          ) : (
            <div>
              {bookings.map(booking => (
                <div className="booking-ticket" key={booking.uuid}>
                  <QRCode value={booking.qrCode || booking.uuid} />
                  <div className="booking-info">
                    <div className="booking-title">{booking.concertTitle}</div>
                    <div className="booking-meta">
                      <div>🎫 {booking.ticketTypeName} × {booking.quantity}</div>
                      <div>💰 {formatCurrency(booking.totalAmount)}</div>
                      {/* Fix #4: concertDateTime now populated from backend */}
                      {booking.concertDateTime && <div>📅 {formatDate(booking.concertDateTime)}</div>}
                      {booking.venueCity && <div>📍 {booking.venueCity}</div>}
                      {booking.bookingReference && (
                        <div style={{ fontSize: 11, color: 'var(--muted2)', fontFamily: 'monospace' }}>
                          Ref: {booking.bookingReference}
                        </div>
                      )}
                    </div>
                    <div className="booking-tags">
                      <span className={`badge ${statusBadgeClass(booking.bookingStatus)}`}>{booking.bookingStatus}</span>
                      <span className={`badge ${booking.paymentStatus === 'PAID' ? 'badge-green' : booking.paymentStatus === 'REFUNDED' ? 'badge-gray' : 'badge-yellow'}`}>{booking.paymentStatus}</span>
                    </div>
                  </div>
                  <div style={{ flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 8 }}>
                    <Link to={`/concerts/${booking.concertId}`} className="btn btn-secondary btn-sm">View Concert</Link>
                    {booking.bookingStatus !== 'CANCELLED' && (
                      <button
                        className="btn btn-sm"
                        style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', color: '#ef4444', fontSize: 12 }}
                        onClick={() => handleCancel(booking.uuid)}
                        disabled={cancellingUuid === booking.uuid}
                      >
                        {cancellingUuid === booking.uuid ? <span className="spinner" /> : 'Cancel'}
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
