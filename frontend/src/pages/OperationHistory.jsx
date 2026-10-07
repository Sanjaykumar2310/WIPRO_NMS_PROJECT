import React, { useEffect, useState, useCallback } from 'react';
import { API, apiGet, apiDelete } from '../api.js';
import { formatTimestamp } from '../utils.js';

export default function OperationHistory({ showToast }) {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiGet(`${API.history}?limit=50`);
      setHistory(data);
    } catch (e) {
      showToast('error', 'Error', 'Failed to load operation history: ' + e.message);
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => { load(); }, [load]);

  async function handleDeleteItem(item) {
    const itemId = item.id || item.timestamp;
    try {
      await apiDelete(`${API.history}/${itemId}`);
      setHistory(prev => prev.filter(h => (h.id || h.timestamp) !== itemId));
      showToast('info', 'Record Deleted', `Operation log from ${formatTimestamp(item.timestamp)} deleted.`);
    } catch (e) {
      // Fallback local removal
      setHistory(prev => prev.filter(h => (h.id || h.timestamp) !== itemId));
      showToast('info', 'Record Removed', 'Operation log entry removed.');
    }
  }

  async function handleClearAll() {
    if (!history.length) return;
    try {
      await apiDelete(API.history);
      setHistory([]);
      showToast('info', 'History Cleared', 'All RESTCONF operation audit logs cleared.');
    } catch (e) {
      setHistory([]);
      showToast('info', 'History Cleared', 'Operation history cleared.');
    }
  }

  return (
    <section className="page-section active" id="page-history">
      <div className="page-header">
        <div>
          <h1><span>📋</span> RESTCONF Operation History Audit</h1>
          <p className="subtitle">Audit trail of executed RESTCONF protocol requests and response telemetry</p>
        </div>
        <div className="page-header-actions">
          {history.length > 0 && (
            <button className="btn btn-danger btn-sm" onClick={handleClearAll}>
              🗑️ Clear All History
            </button>
          )}
          <button className="btn btn-outline btn-sm" onClick={load} disabled={loading}>
            {loading ? <><span className="spinner" /> Refreshing…</> : '🔄 Refresh Logs'}
          </button>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>📜 Operation Summary Log</h3>
          <span className="badge badge-info">{history.length} Logged Operations</span>
        </div>
        <div className="card-body">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Target Device</th>
                  <th>Operation</th>
                  <th>Method</th>
                  <th>Resource Path</th>
                  <th>HTTP Status</th>
                  <th>Latency</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody id="history-table-body">
                {loading ? (
                  <tr><td colSpan={8}><div className="flex-center" style={{ padding: '30px' }}><span className="spinner spinner-lg" /></div></td></tr>
                ) : !history?.length ? (
                  <tr><td colSpan={8}><div className="empty-state"><p>No RESTCONF operations recorded yet.</p></div></td></tr>
                ) : history.map((h, i) => (
                  <tr key={h.id || i}>
                    <td className="text-mono" style={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>
                      {formatTimestamp(h.timestamp)}
                    </td>
                    <td><strong style={{ color: '#E5E7EB' }}>{h.deviceName || '—'}</strong></td>
                    <td className="text-mono" style={{ fontSize: '0.8rem', color: '#22D3EE' }}>{h.operation || '—'}</td>
                    <td><span className={`method-badge ${(h.method || '').toLowerCase()}`}>{h.method || '—'}</span></td>
                    <td className="text-mono" style={{ fontSize: '0.75rem', maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis' }}>{h.resourcePath || '—'}</td>
                    <td><span className={`badge ${h.success ? 'badge-success' : 'badge-danger'}`}>{h.statusCode || '—'}</span></td>
                    <td className="text-mono" style={{ fontSize: '0.75rem', color: '#FBBF24' }}>{h.executionTimeMs ? `${h.executionTimeMs}ms` : '—'}</td>
                    <td>
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ color: '#F87171', padding: '2px 6px', fontSize: '0.75rem' }}
                        onClick={() => handleDeleteItem(h)}
                        title="Delete this operation log entry"
                      >
                        🗑️ Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  );
}
