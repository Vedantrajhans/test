import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { concertApi, extractErrorMessage } from '../api';
import { fromLocalDateTimeInput, toLocalDateTimeInput } from '../utils/format';
import Sidebar from '../components/common/Sidebar';

const genres = ['Rock', 'Pop', 'Jazz', 'Classical', 'Electronic', 'Hip-Hop', 'Metal', 'Folk', 'Indie', 'Fusion', 'Bollywood', 'Classical Indian'];
const emptyTicket = { name: '', price: '', quantity: '' };

export default function CreateConcertPage({ isEdit }) {
  const [form, setForm] = useState({
    title: '',
    description: '',
    genre: genres[0],
    dateTime: '',
    endTime: '',
    totalCapacity: '',
    ticketSaleStart: '',
    ticketSaleEnd: '',
    imageUrl: '',
    venueAddress: '',
    venueCity: '',
    lineup: '',
    ticketTypes: [{ ...emptyTicket }],
  });
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(Boolean(isEdit));
  const [publish, setPublish] = useState(false);
  const navigate = useNavigate();
  const { id } = useParams();

  useEffect(() => {
    if (!isEdit || !id) return;
    concertApi.getById(id).then(({ data }) => {
      setForm({
        title: data.title || '',
        description: data.description || '',
        genre: data.genre || genres[0],
        dateTime: toLocalDateTimeInput(data.dateTime),
        endTime: toLocalDateTimeInput(data.endTime),
        totalCapacity: data.totalCapacity || '',
        ticketSaleStart: toLocalDateTimeInput(data.ticketSaleStart),
        ticketSaleEnd: toLocalDateTimeInput(data.ticketSaleEnd),
        imageUrl: data.imageUrl || '',
        venueAddress: data.venue?.address || '',
        venueCity: data.venue?.city || '',
        lineup: data.lineup || '',
        ticketTypes: data.ticketTypes?.length
          ? data.ticketTypes.map(t => ({ name: t.name, price: t.price, quantity: t.availableQuantity }))
          : [{ ...emptyTicket }],
      });
    }).catch(err => toast.error(extractErrorMessage(err, 'Could not load concert')))
      .finally(() => setLoading(false));
  }, [isEdit, id]);

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));

  const setTicket = (i, k) => e => {
    setForm(f => {
      const tt = [...f.ticketTypes];
      tt[i] = { ...tt[i], [k]: e.target.value };
      return { ...f, ticketTypes: tt };
    });
  };

  const addTicket = () => setForm(f => ({ ...f, ticketTypes: [...f.ticketTypes, { ...emptyTicket }] }));
  const removeTicket = i => setForm(f => ({ ...f, ticketTypes: f.ticketTypes.filter((_, idx) => idx !== i) }));

  const onSubmit = async (e, shouldPublish = false) => {
    e && e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        title: form.title,
        description: form.description,
        genre: form.genre,
        dateTime: fromLocalDateTimeInput(form.dateTime),
        endTime: fromLocalDateTimeInput(form.endTime),
        totalCapacity: Number(form.totalCapacity),
        ticketSaleStart: fromLocalDateTimeInput(form.ticketSaleStart),
        ticketSaleEnd: fromLocalDateTimeInput(form.ticketSaleEnd),
        imageUrl: form.imageUrl || null,
        venue: { address: form.venueAddress, city: form.venueCity },
        lineup: form.lineup,
        ticketTypes: form.ticketTypes
          .filter(t => t.name && t.price)
          .map(t => ({ name: t.name, price: Number(t.price), quantity: Number(t.quantity) || 0 })),
      };

      let saved;
      if (isEdit && id) {
        const { data } = await concertApi.update(id, payload);
        saved = data;
        toast.success('Concert updated');
      } else {
        const { data } = await concertApi.create(payload);
        saved = data;
        toast.success('Concert created');
      }

      if (shouldPublish && saved?.id) {
        await concertApi.publish(saved.id);
        toast.success('Concert published!');
      }

      navigate('/organizer/concerts');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Save failed'));
    } finally { setSaving(false); }
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
              <div className="page-title">{isEdit ? 'Edit Concert' : 'Create Concert'}</div>
              <div className="page-subtitle">{isEdit ? 'Update concert details' : 'Fill in the details and save or publish'}</div>
            </div>
          </div>

          <form onSubmit={onSubmit} style={{ maxWidth: 760 }}>
            {/* Basic info */}
            <div className="card card-p mb-16">
              <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>Basic Information</div>
              <div className="form-group">
                <label className="form-label">Concert title *</label>
                <input className="input" value={form.title} onChange={set('title')} placeholder="e.g. Arijit Singh Live 2025" required />
              </div>
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Genre</label>
                  <select className="input select" value={form.genre} onChange={set('genre')}>
                    {genres.map(g => <option key={g} value={g}>{g}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Total capacity</label>
                  <input className="input" type="number" min="1" value={form.totalCapacity} onChange={set('totalCapacity')} placeholder="e.g. 5000" />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea className="input textarea" value={form.description} onChange={set('description')} placeholder="Describe the concert…" />
              </div>
              <div className="form-group">
                <label className="form-label">Lineup / Artists</label>
                <input className="input" value={form.lineup} onChange={set('lineup')} placeholder="e.g. Arijit Singh, Shreya Ghoshal" />
              </div>
              <div className="form-group">
                <label className="form-label">Concert photo URL</label>
                <input className="input" type="url" value={form.imageUrl} onChange={set('imageUrl')} placeholder="https://…" />
              </div>
            </div>

            {/* Venue */}
            <div className="card card-p mb-16">
              <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>Venue</div>
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Venue address</label>
                  <input className="input" value={form.venueAddress} onChange={set('venueAddress')} placeholder="Stadium name, street…" />
                </div>
                <div className="form-group">
                  <label className="form-label">City</label>
                  <input className="input" value={form.venueCity} onChange={set('venueCity')} placeholder="Mumbai" />
                </div>
              </div>
            </div>

            {/* Dates */}
            <div className="card card-p mb-16">
              <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>Dates & Times</div>
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Start date & time *</label>
                  <input className="input" type="datetime-local" value={form.dateTime} onChange={set('dateTime')} required />
                </div>
                <div className="form-group">
                  <label className="form-label">End date & time</label>
                  <input className="input" type="datetime-local" value={form.endTime} onChange={set('endTime')} />
                </div>
                <div className="form-group">
                  <label className="form-label">Ticket sale starts</label>
                  <input className="input" type="datetime-local" value={form.ticketSaleStart} onChange={set('ticketSaleStart')} />
                </div>
                <div className="form-group">
                  <label className="form-label">Ticket sale ends</label>
                  <input className="input" type="datetime-local" value={form.ticketSaleEnd} onChange={set('ticketSaleEnd')} />
                </div>
              </div>
            </div>

            {/* Ticket types */}
            <div className="card card-p mb-16">
              <div className="flex-between mb-14">
                <div style={{ fontWeight: 700, fontSize: 14 }}>Ticket Types</div>
                <button type="button" className="btn btn-secondary btn-sm" onClick={addTicket}>+ Add type</button>
              </div>
              {form.ticketTypes.map((tt, i) => (
                <div key={i} style={{ background: 'var(--bg3)', borderRadius: 8, padding: '12px 14px', marginBottom: 10 }}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr auto', gap: 10, alignItems: 'end' }}>
                    <div>
                      <label className="form-label">Name</label>
                      <input className="input" placeholder="e.g. General" value={tt.name} onChange={setTicket(i, 'name')} required />
                    </div>
                    <div>
                      <label className="form-label">Price (₹)</label>
                      <input className="input" type="number" min="0" placeholder="999" value={tt.price} onChange={setTicket(i, 'price')} required />
                    </div>
                    <div>
                      <label className="form-label">Quantity</label>
                      <input className="input" type="number" min="0" placeholder="500" value={tt.quantity} onChange={setTicket(i, 'quantity')} />
                    </div>
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => removeTicket(i)} disabled={form.ticketTypes.length === 1} style={{ alignSelf: 'flex-end' }}>✕</button>
                  </div>
                </div>
              ))}
            </div>

            {/* Actions */}
            <div className="flex" style={{ gap: 10 }}>
              <button type="button" className="btn btn-secondary" onClick={() => navigate('/organizer/concerts')}>Cancel</button>
              <button type="submit" className="btn btn-secondary" disabled={saving}>
                {saving ? <span className="spinner" /> : isEdit ? 'Save changes' : 'Save as upcoming'}
              </button>
              {!isEdit && (
                <button type="button" className="btn btn-primary" disabled={saving} onClick={() => onSubmit(null, true)}>
                  {saving ? <span className="spinner" /> : 'Save & Publish'}
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
