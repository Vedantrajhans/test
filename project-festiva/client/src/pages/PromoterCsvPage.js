import React, { useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { extractErrorMessage, promoterApi } from '../api';
import { downloadBlob } from '../utils/app';
import Sidebar from '../components/common/Sidebar';

export default function PromoterCsvPage() {
  const [importSummary, setImportSummary] = useState(null);
  const [importing, setImporting] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const fileRef = useRef(null);

  // Export filter state
  const [filters, setFilters] = useState({ city: '', state: '', organizerType: '', search: '' });
  const setFilter = k => e => setFilters(f => ({ ...f, [k]: e.target.value }));

  const handleFile = async (file) => {
    if (!file || !file.name.endsWith('.csv')) { toast.error('Please select a .csv file'); return; }
    setImporting(true);
    try {
      const { data } = await promoterApi.importOrganizers(file);
      setImportSummary(data);
      toast.success('CSV imported successfully');
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Import failed'));
    } finally { setImporting(false); }
  };

  const onFileChange = e => { handleFile(e.target.files?.[0]); e.target.value = ''; };

  const onDrop = (e) => {
    e.preventDefault(); setDragOver(false);
    handleFile(e.dataTransfer.files?.[0]);
  };

  const onExport = async () => {
    setExporting(true);
    try {
      // Build params — only include non-empty filters
      const params = {};
      if (filters.city.trim())          params.city          = filters.city.trim();
      if (filters.state.trim())         params.state         = filters.state.trim();
      if (filters.organizerType.trim()) params.organizerType = filters.organizerType.trim();
      if (filters.search.trim())        params.search        = filters.search.trim();

      const { data } = await promoterApi.exportOrganizers(params);
      const filename = buildFilename(params);
      downloadBlob(new Blob([data], { type: 'text/csv' }), filename);
      toast.success(`Exported ${filename}`);
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Export failed'));
    } finally { setExporting(false); }
  };

  const clearFilters = () => setFilters({ city: '', state: '', organizerType: '', search: '' });
  const hasFilters = Object.values(filters).some(v => v.trim() !== '');

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-content">
        <div className="page-wrap">
          <div className="page-header">
            <div>
              <div className="page-title">Import / Export CSV</div>
              <div className="page-subtitle">Bulk create or update organizers via CSV</div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, maxWidth: 860 }}>
            {/* IMPORT */}
            <div className="card card-p">
              <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 6 }}>📥 Import Organizers</div>
              <div style={{ fontSize: 13, color: 'var(--muted)', lineHeight: 1.6, marginBottom: 16 }}>
                Upload a CSV to bulk-create or update organizers. Existing emails will be updated; new ones created.
              </div>

              <div
                className="csv-zone"
                onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={onDrop}
                onClick={() => fileRef.current?.click()}
                style={dragOver ? { borderColor: 'var(--accent)', background: 'rgba(255,92,53,0.05)' } : {}}
              >
                <div style={{ fontSize: '1.8rem', marginBottom: 8 }}>📄</div>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>Drop your CSV here</div>
                <div style={{ fontSize: 12, color: 'var(--muted2)' }}>or click to browse</div>
                {importing && <div style={{ marginTop: 10 }}><span className="spinner" /></div>}
              </div>
              <input ref={fileRef} type="file" accept=".csv" hidden onChange={onFileChange} />

              {importSummary && (
                <div className="banner banner-success mt-12">
                  ✓ {importSummary.createdCount || 0} created · {importSummary.updatedCount || 0} updated
                </div>
              )}

              <div style={{ marginTop: 14, background: 'var(--bg3)', borderRadius: 8, padding: '12px 14px' }}>
                <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--muted)', marginBottom: 8, letterSpacing: '0.05em', textTransform: 'uppercase' }}>Expected CSV columns</div>
                <code style={{ fontSize: 11.5, color: 'var(--accent2)', lineHeight: 1.7, display: 'block' }}>
                  email, firstName, lastName, organizerType,<br />
                  companyName, city, state
                </code>
              </div>
            </div>

            {/* EXPORT */}
            <div className="card card-p">
              <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 6 }}>📤 Export Organizers</div>
              <div style={{ fontSize: 13, color: 'var(--muted)', lineHeight: 1.6, marginBottom: 14 }}>
                Filter by city, state, type or name before exporting — or leave blank to export all.
              </div>

              {/* Filter inputs */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 10 }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: 11 }}>City</label>
                  <input
                    className="input"
                    style={{ fontSize: 13, padding: '7px 10px' }}
                    placeholder="e.g. Mumbai"
                    value={filters.city}
                    onChange={setFilter('city')}
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: 11 }}>State</label>
                  <input
                    className="input"
                    style={{ fontSize: 13, padding: '7px 10px' }}
                    placeholder="e.g. Maharashtra"
                    value={filters.state}
                    onChange={setFilter('state')}
                  />
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 14 }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: 11 }}>Type</label>
                  <input
                    className="input"
                    style={{ fontSize: 13, padding: '7px 10px' }}
                    placeholder="e.g. VENUE"
                    value={filters.organizerType}
                    onChange={setFilter('organizerType')}
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: 11 }}>Search (name / email)</label>
                  <input
                    className="input"
                    style={{ fontSize: 13, padding: '7px 10px' }}
                    placeholder="Any keyword"
                    value={filters.search}
                    onChange={setFilter('search')}
                  />
                </div>
              </div>

              {hasFilters && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                  <div style={{ fontSize: 12, color: 'var(--accent2)', fontWeight: 600 }}>🔍 Filters active</div>
                  <button
                    onClick={clearFilters}
                    style={{ fontSize: 11, background: 'none', border: '1px solid var(--border2)', borderRadius: 6, padding: '2px 8px', color: 'var(--muted)', cursor: 'pointer' }}
                  >
                    Clear
                  </button>
                </div>
              )}

              <button className="btn btn-primary w-full" style={{ justifyContent: 'center' }} onClick={onExport} disabled={exporting}>
                {exporting ? <span className="spinner" /> : `⬇ Download${hasFilters ? ' Filtered' : ''} CSV`}
              </button>

              <div style={{ marginTop: 16, background: 'var(--bg3)', borderRadius: 8, padding: '12px 14px' }}>
                <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--muted)', marginBottom: 8, letterSpacing: '0.05em', textTransform: 'uppercase' }}>Export includes</div>
                <div style={{ fontSize: 12.5, color: 'var(--muted)', lineHeight: 1.7 }}>
                  ID, email, name, type, company, city, state, status, created date
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function buildFilename(params) {
  const parts = ['organizers'];
  if (params.city)          parts.push(params.city.replace(/\s+/g, '-'));
  if (params.state)         parts.push(params.state.replace(/\s+/g, '-'));
  if (params.organizerType) parts.push(params.organizerType);
  if (params.search)        parts.push('search');
  return parts.join('_') + '.csv';
}
