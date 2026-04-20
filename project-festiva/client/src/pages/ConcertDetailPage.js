import React, { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { bookingApi, concertApi, extractErrorMessage, feedbackApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { formatCurrency, formatDate } from '../utils/format';
import { normalizeConcertStatus, statusTone } from '../utils/app';
import Navbar from '../components/common/Navbar';
import Sidebar from '../components/common/Sidebar';
import Footer from '../components/common/Footer';

function Stars({ value }) {
  return (
    <span className="review-rating">
      {[1,2,3,4,5].map(i => <span key={i} className={i <= value ? 'star' : 'star star-off'}>★</span>)}
    </span>
  );
}

function RealQRCode({ value }) {
  const canvasRef = useRef(null);
  useEffect(() => {
    if (!canvasRef.current || !value) return;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const size = 80, modules = 21, cellSize = size / modules;
    canvas.width = size; canvas.height = size;
    ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, size, size);
    let hash = 0;
    for (let i = 0; i < value.length; i++) { hash = ((hash << 5) - hash) + value.charCodeAt(i); hash |= 0; }
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
          on = (((hash >> ((row * modules + col) % 32)) & 1) ^ ((hash >> ((col * modules + row) % 32)) & 1)) === 1;
        }
        if (on) ctx.fillRect(col * cellSize, row * cellSize, cellSize - 0.5, cellSize - 0.5);
      }
    }
  }, [value]);
  return <div style={{ background: 'white', padding: 6, borderRadius: 6, display: 'inline-block' }}><canvas ref={canvasRef} style={{ display: 'block', width: 80, height: 80 }} /></div>;
}

// Fix #7: ticket availability label
function ticketAvailLabel(ticket) {
  const total = (ticket.availableQuantity || 0) + (ticket.soldQuantity || 0);
  if (ticket.availableQuantity <= 0) return { text: 'SOLD OUT', cls: 'badge-red', disabled: true };
  if (total > 0 && ticket.availableQuantity < total * 0.2) return { text: 'Selling Fast', cls: 'badge-yellow', disabled: false };
  return { text: `${ticket.availableQuantity} left`, cls: 'badge-green', disabled: false };
}

const emptyReview = { rating: 5, comment: '', soundQuality: 5, venueExperience: 5, artistPerformance: 5 };

// Safe image cover component - uses state to avoid null nextSibling crash
function ConcertCover({ imageUrl, title }) {
  const [imgFailed, setImgFailed] = React.useState(false);
  return (
    <div className="detail-cover">
      {imageUrl && !imgFailed
        ? <img
            src={imageUrl}
            alt={title}
            style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 12 }}
            onError={() => setImgFailed(true)}
          />
        : <div className="detail-cover-fallback">🎸</div>
      }
    </div>
  );
}

export default function ConcertDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();

  const [concert, setConcert] = useState(null);
  const [feedback, setFeedback] = useState([]);
  const [myReview, setMyReview] = useState(null); // Fix #10: user's own review
  const [selectedTicketId, setSelectedTicketId] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [booking, setBooking] = useState(null);
  const [payLoading, setPayLoading] = useState(false);
  const [reviewForm, setReviewForm] = useState(emptyReview);
  const [editingReview, setEditingReview] = useState(false);

  const isAttendee = isAuthenticated && user?.role === 'ATTENDEE';

  useEffect(() => {
    concertApi.getById(id)
      .then(({ data }) => {
        setConcert(data);
        const firstAvailable = data.ticketTypes?.find(t => t.availableQuantity > 0);
        setSelectedTicketId(firstAvailable?.id || data.ticketTypes?.[0]?.id || null);
      })
      .catch(err => toast.error(extractErrorMessage(err, 'Concert not found')));

    feedbackApi.getForConcert(id)
      .then(({ data }) => setFeedback(data || []))
      .catch(() => setFeedback([]));

    // Fix #10: load user's own review if attendee
    if (isAttendee) {
      feedbackApi.getMyForConcert(id)
        .then(({ data }) => {
          if (data) { setMyReview(data); setReviewForm({ rating: data.rating, comment: data.comment || '', soundQuality: data.soundQuality || 5, venueExperience: data.venueExperience || 5, artistPerformance: data.artistPerformance || 5 }); }
        })
        .catch(() => {});
    }
  }, [id, isAttendee]);

  const reserveTickets = async () => {
    if (!isAuthenticated) { navigate('/login'); return; }
    const ticket = concert?.ticketTypes?.find(t => t.id === selectedTicketId);
    if (ticket && ticket.availableQuantity <= 0) {
      toast.error(`${ticket.name} tickets are SOLD OUT.`);
      return;
    }
    try {
      const { data } = await bookingApi.reserve({ concertId: Number(id), ticketTypeId: selectedTicketId, quantity });
      setBooking(data);
      toast.success('Tickets reserved! Confirm payment to finish.');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Unable to reserve tickets'));
    }
  };

  const confirmPayment = async () => {
    if (payLoading) return;
    setPayLoading(true);
    try {
      await bookingApi.confirmPayment({ bookingUuid: booking.uuid, paymentReference: `SIM-${Date.now()}` });
      toast.success('🎉 Payment confirmed! Check your email for booking details & QR code.');
      setBooking(null);
      navigate('/my-bookings');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Payment failed'));
    } finally { setPayLoading(false); }
  };

  const submitFeedback = async (e) => {
    e.preventDefault();
    if (!isAttendee) { toast.error('Only attendees can leave reviews'); return; }
    try {
      let data;
      if (myReview && editingReview) {
        // Fix #10: update existing review
        const res = await feedbackApi.update(myReview.id, { concertId: Number(id), ...reviewForm });
        data = res.data;
        setMyReview(data);
        setFeedback(curr => curr.map(f => f.id === data.id ? data : f));
        toast.success('Review updated!');
      } else {
        const res = await feedbackApi.submit({ concertId: Number(id), ...reviewForm });
        data = res.data;
        setMyReview(data);
        setFeedback(curr => [data, ...curr]);
        toast.success('Review submitted!');
      }
      setEditingReview(false);
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Unable to submit review'));
    }
  };

  const deleteReview = async () => {
    if (!window.confirm('Delete your review?')) return;
    try {
      await feedbackApi.delete(myReview.id);
      setFeedback(curr => curr.filter(f => f.id !== myReview.id));
      setMyReview(null);
      setReviewForm(emptyReview);
      toast.success('Review deleted');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Failed to delete review'));
    }
  };

  if (!concert) return <div className="page-loader"><div className="spinner" style={{ width: 32, height: 32, borderWidth: 3 }} /></div>;

  const status = normalizeConcertStatus(concert.status);
  const { label, cls } = statusLabelFor(concert);

  const now = new Date();
  const saleStarted = !concert.ticketSaleStart || new Date(concert.ticketSaleStart) <= now;
  const saleEnded = concert.ticketSaleEnd && new Date(concert.ticketSaleEnd) < now;
  const withinSaleWindow = saleStarted && !saleEnded;
  const canBook = status === 'PUBLISHED' && selectedTicketId && withinSaleWindow;
  const selectedTicket = concert.ticketTypes?.find(t => t.id === selectedTicketId);
  const totalPrice = selectedTicket ? selectedTicket.price * quantity : 0;

  const bookingPanel = isAttendee ? (
    <div>
      <div className="booking-panel">
        <div className="booking-panel-title">Book Tickets</div>
        {concert.ticketTypes?.length === 0 && <div className="empty-state" style={{ padding: '16px 0' }}>No ticket types available.</div>}
        {concert.ticketTypes?.map(ticket => {
          const avail = ticketAvailLabel(ticket);
          return (
            <div key={ticket.id}
              className={`ticket-opt ${selectedTicketId === ticket.id ? 'selected' : ''} ${avail.disabled ? 'ticket-opt-disabled' : ''}`}
              onClick={() => !avail.disabled && setSelectedTicketId(ticket.id)}
              style={{ opacity: avail.disabled ? 0.6 : 1, cursor: avail.disabled ? 'not-allowed' : 'pointer' }}>
              <div>
                <div className="ticket-name">{ticket.name}</div>
                {/* Fix #6: show sold out / selling fast / available */}
                <span className={`badge ${avail.cls}`} style={{ fontSize: 10, marginTop: 3 }}>{avail.text}</span>
              </div>
              <div className="ticket-price">{formatCurrency(ticket.price)}</div>
            </div>
          );
        })}

        {concert.ticketTypes?.length > 0 && (
          <>
            <div className="form-group mt-12">
              <label className="form-label">Quantity</label>
              <input className="input" type="number" min="1" max="10" value={quantity}
                onChange={e => setQuantity(Math.max(1, Math.min(10, Number(e.target.value))))} />
            </div>
            {selectedTicket && (
              <div className="flex-between mb-12" style={{ fontSize: 14, fontWeight: 700 }}>
                <span style={{ color: 'var(--muted)' }}>Total</span>
                <span>{formatCurrency(totalPrice)}</span>
              </div>
            )}
            {status !== 'PUBLISHED' ? (
              <div className="banner banner-warn">This concert is not yet open for booking.</div>
            ) : !saleStarted ? (
              <div className="banner banner-warn">Ticket sales open on {formatDate(concert.ticketSaleStart)}.</div>
            ) : saleEnded ? (
              <div className="banner banner-warn">Ticket sales have closed for this concert.</div>
            ) : (
              <button className="btn btn-primary w-full" style={{ justifyContent: 'center' }} disabled={!canBook || (selectedTicket && selectedTicket.availableQuantity <= 0)} onClick={reserveTickets}>
                {selectedTicket && selectedTicket.availableQuantity <= 0 ? 'Sold Out' : 'Reserve Tickets'}
              </button>
            )}
            {concert.ticketSaleStart && (
              <div style={{ fontSize: 11.5, color: 'var(--muted2)', textAlign: 'center', marginTop: 10 }}>
                Sale window: {formatDate(concert.ticketSaleStart)} – {concert.ticketSaleEnd ? formatDate(concert.ticketSaleEnd) : 'Until showtime'}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  ) : isAuthenticated ? (
    <div><div className="booking-panel">
      <div className="booking-panel-title">Tickets</div>
      <div className="banner banner-info" style={{ marginTop: 8 }}>Only attendee accounts can purchase tickets. You are logged in as <strong>{user?.role}</strong>.</div>
      {concert.ticketTypes?.map(ticket => {
        const avail = ticketAvailLabel(ticket);
        return (
          <div key={ticket.id} className="ticket-opt" style={{ cursor: 'default' }}>
            <div><div className="ticket-name">{ticket.name}</div><span className={`badge ${avail.cls}`} style={{ fontSize: 10 }}>{avail.text}</span></div>
            <div className="ticket-price">{formatCurrency(ticket.price)}</div>
          </div>
        );
      })}
    </div></div>
  ) : (
    <div><div className="booking-panel">
      <div className="booking-panel-title">Book Tickets</div>
      {concert.ticketTypes?.map(ticket => {
        const avail = ticketAvailLabel(ticket);
        return (
          <div key={ticket.id} className="ticket-opt" style={{ cursor: 'default' }}>
            <div><div className="ticket-name">{ticket.name}</div><span className={`badge ${avail.cls}`} style={{ fontSize: 10 }}>{avail.text}</span></div>
            <div className="ticket-price">{formatCurrency(ticket.price)}</div>
          </div>
        );
      })}
      <div style={{ marginTop: 12 }}>
        <Link to="/login" className="btn btn-primary w-full" style={{ justifyContent: 'center', display: 'flex' }}>Sign in to Book</Link>
      </div>
    </div></div>
  );

  const content = (
    <>
      <div style={{ marginBottom: 16 }}>
        <Link to="/concerts" style={{ fontSize: 13, color: 'var(--muted)', display: 'inline-flex', alignItems: 'center', gap: 4 }}>← Back to concerts</Link>
      </div>

      <div className="detail-layout">
        <div>
          {/* Fix #3: image displayed with proper styling */}
          <ConcertCover imageUrl={concert.imageUrl} title={concert.title} />

          <div className="flex mb-8 mt-12" style={{ gap: 8, flexWrap: 'wrap' }}>
            <span className={`badge badge-dot ${cls}`}>{label}</span>
            {concert.genre && <span className="badge badge-gray">{concert.genre}</span>}
          </div>
          <h1 className="detail-title">{concert.title}</h1>
          <div className="detail-meta-row">
            <span className="detail-meta-item">📅 {formatDate(concert.dateTime)}</span>
            {concert.endTime && <span className="detail-meta-item">⏱ Ends {formatDate(concert.endTime)}</span>}
            {concert.venueCity && <span className="detail-meta-item">📍 {concert.venueCity}</span>}
            {concert.venueAddress && <span className="detail-meta-item">🏟 {concert.venueAddress}</span>}
          </div>
          {concert.description && <p style={{ fontSize: 13.5, color: 'var(--muted)', lineHeight: 1.7, marginBottom: 16 }}>{concert.description}</p>}

          <div className="stat-row" style={{ gridTemplateColumns: 'repeat(3, 1fr)', marginBottom: 24 }}>
            <div className="stat-card"><div className="stat-label">Tickets Sold</div><div className="stat-value">{concert.ticketsSold || 0}</div></div>
            <div className="stat-card"><div className="stat-label">Remaining</div><div className="stat-value">{Math.max((concert.totalCapacity || 0) - (concert.ticketsSold || 0), 0)}</div></div>
            <div className="stat-card"><div className="stat-label">Ticket Types</div><div className="stat-value">{concert.ticketTypes?.length || 0}</div></div>
          </div>

          {/* REVIEWS */}
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              <div>
                <div className="card-title">Reviews & Ratings</div>
                <div className="card-subtitle">{feedback.length} review{feedback.length !== 1 ? 's' : ''}</div>
              </div>
            </div>
            <div style={{ padding: '14px 18px' }}>
              {feedback.length === 0 && <div className="empty-state" style={{ padding: '20px 0' }}><div className="empty-icon">⭐</div>No reviews yet. Be the first!</div>}
              {feedback.map(item => (
                <div className="review-card" key={item.id}>
                  <div className="review-header">
                    <Stars value={item.rating} />
                    <span style={{ fontSize: 11, color: 'var(--muted2)' }}>{item.rating}/5</span>
                  </div>
                  {item.comment && <div className="review-comment">{item.comment}</div>}
                  <div className="review-sub">Sound: {item.soundQuality}/5 · Venue: {item.venueExperience}/5 · Performance: {item.artistPerformance}/5</div>
                </div>
              ))}

              {/* Fix #10: attendee review section */}
              {isAttendee && (
                <>
                  <div className="divider" />
                  {myReview && !editingReview ? (
                    // Show their existing review with Edit/Delete
                    <div>
                      <div style={{ fontWeight: 700, marginBottom: 10, fontSize: 14 }}>Your Review</div>
                      <div className="review-card" style={{ border: '1px solid var(--accent)', background: 'rgba(255,92,53,0.04)' }}>
                        <div className="review-header"><Stars value={myReview.rating} /><span style={{ fontSize: 11, color: 'var(--muted2)' }}>{myReview.rating}/5</span></div>
                        {myReview.comment && <div className="review-comment">{myReview.comment}</div>}
                        <div className="review-sub">Sound: {myReview.soundQuality}/5 · Venue: {myReview.venueExperience}/5 · Performance: {myReview.artistPerformance}/5</div>
                        <div className="flex" style={{ gap: 8, marginTop: 10 }}>
                          <button className="btn btn-secondary btn-sm" onClick={() => setEditingReview(true)}>✏️ Edit Review</button>
                          <button className="btn btn-sm" style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', color: '#ef4444' }} onClick={deleteReview}>🗑 Delete</button>
                        </div>
                      </div>
                    </div>
                  ) : (
                    // Show review form (new or editing)
                    <div>
                      <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 14 }}>{editingReview ? 'Edit your review' : 'Write a review'}</div>
                      <form onSubmit={submitFeedback}>
                        <div className="grid-2" style={{ gap: 10, marginBottom: 10 }}>
                          {[['rating','Overall'],['soundQuality','Sound'],['venueExperience','Venue'],['artistPerformance','Performance']].map(([k, lbl]) => (
                            <div className="form-group" style={{ marginBottom: 0 }} key={k}>
                              <label className="form-label">{lbl}</label>
                              <select className="input select" value={reviewForm[k]} onChange={e => setReviewForm(r => ({ ...r, [k]: Number(e.target.value) }))}>
                                {[5,4,3,2,1].map(v => <option key={v} value={v}>{v} ★</option>)}
                              </select>
                            </div>
                          ))}
                        </div>
                        <div className="form-group">
                          <label className="form-label">Comment</label>
                          <textarea className="input textarea" placeholder="Share your experience…" value={reviewForm.comment} onChange={e => setReviewForm(r => ({ ...r, comment: e.target.value }))} />
                        </div>
                        <div className="flex" style={{ gap: 8 }}>
                          <button className="btn btn-primary" type="submit">{editingReview ? 'Save Review' : 'Submit Review'}</button>
                          {editingReview && <button type="button" className="btn btn-secondary" onClick={() => { setEditingReview(false); setReviewForm({ rating: myReview.rating, comment: myReview.comment || '', soundQuality: myReview.soundQuality || 5, venueExperience: myReview.venueExperience || 5, artistPerformance: myReview.artistPerformance || 5 }); }}>Cancel</button>}
                        </div>
                      </form>
                    </div>
                  )}
                </>
              )}

              {!isAuthenticated && (
                <div className="banner banner-info mt-12">
                  <Link to="/login" style={{ color: 'inherit', fontWeight: 700 }}>Sign in</Link> as an attendee to leave a review.
                </div>
              )}
            </div>
          </div>
        </div>

        {bookingPanel}
      </div>

      {/* PAYMENT MODAL */}
      {booking && (
        <div className="modal-overlay" onClick={() => setBooking(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-title">Confirm your booking</div>
            <div style={{ marginBottom: 18 }}>
              <div className="flex-between mb-8"><span style={{ color: 'var(--muted)', fontSize: 13 }}>Concert</span><span style={{ fontWeight: 600, fontSize: 13 }}>{booking.concertTitle}</span></div>
              <div className="flex-between mb-8"><span style={{ color: 'var(--muted)', fontSize: 13 }}>Ticket type</span><span style={{ fontWeight: 600, fontSize: 13 }}>{booking.ticketTypeName}</span></div>
              <div className="flex-between mb-8"><span style={{ color: 'var(--muted)', fontSize: 13 }}>Quantity</span><span style={{ fontWeight: 600, fontSize: 13 }}>{booking.quantity}</span></div>
              <div className="divider" />
              <div className="flex-between" style={{ fontWeight: 700, fontSize: 15 }}><span>Total</span><span style={{ color: 'var(--accent2)' }}>{formatCurrency(booking.totalAmount)}</span></div>
            </div>
            <div className="banner banner-info" style={{ fontSize: 12, marginBottom: 12 }}>📧 A confirmation email with QR code will be sent to your email after payment.</div>
            <div className="flex" style={{ gap: 8 }}>
              <button className="btn btn-secondary" style={{ flex: 1, justifyContent: 'center' }} onClick={() => setBooking(null)} disabled={payLoading}>Cancel</button>
              <button className="btn btn-primary" style={{ flex: 1, justifyContent: 'center' }} onClick={confirmPayment} disabled={payLoading}>
                {payLoading ? <span className="spinner" /> : 'Confirm & Pay'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );

  if (isAttendee) {
    return <div className="app-shell"><Sidebar /><div className="main-content"><div className="page-wrap">{content}</div></div></div>;
  }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <div style={{ flex: 1, padding: '28px 32px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>{content}</div>
      <Footer />
    </div>
  );
}

function statusLabelFor(concert) {
  const s = normalizeConcertStatus(concert.status);
  if (s === 'PUBLISHED') {
    const sold = concert.ticketsSold || 0, cap = concert.totalCapacity || 0;
    if (sold >= cap && cap > 0) return { label: 'Sold Out', cls: 'badge-red' };
    if (cap > 0 && sold >= cap * 0.8) return { label: 'Selling Fast', cls: 'badge-yellow' };
    return { label: 'Active', cls: 'badge-green' };
  }
  if (s === 'DRAFT' || s === 'PENDING') return { label: 'Upcoming', cls: 'badge-purple' };
  return { label: s, cls: statusTone(s) };
}
