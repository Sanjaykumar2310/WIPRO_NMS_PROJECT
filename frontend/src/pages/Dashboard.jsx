import React, { useEffect, useState, useCallback } from 'react';
import { API, apiGet } from '../api.js';
import { formatTime } from '../utils.js';
import AnimatedCounter from '../components/AnimatedCounter.jsx';
import NetworkTopology from '../components/NetworkTopology.jsx';

export default function Dashboard({ showToast, navigateTo }) {
  const [stats, setStats]   = useState(null);
  const [devices, setDevices] = useState([]);
  const [ops, setOps]       = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const s = await apiGet(API.dashboard);
      setStats(s);
      setOps(s.recentOperations || []);

      try {
        const ai = await apiGet(`${API.ai}/status`);
        setStats(prev => ({ ...prev, aiAnalyses: ai.totalAnalyses ?? 0 }));
      } catch { /* ignore */ }

      const devs = await apiGet(API.devices);
      setDevices(devs);
    } catch (e) {
      showToast('error', 'Error', 'Failed to load RESTCONF telemetry: ' + e.message);
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => { load(); }, [load]);

  const successOps = ops.filter(o => o.success).length;
  const failOps = ops.filter(o => !o.success).length;

  const statCards = [
    { color: 'green',  icon: '🖥️', value: stats?.totalDevices ?? devices.length,  label: 'Connected Devices' },
    { color: 'blue',   icon: '⚡', value: stats?.totalOperations ?? ops.length,     label: 'CRUD Operations' },
    { color: 'cyan',   icon: '✅', value: successOps || (stats?.onlineDevices ?? 0), label: 'Successful Requests' },
    { color: 'orange', icon: '❌', value: failOps || (stats?.offlineDevices ?? 0),   label: 'Failed Requests' },
    { color: 'purple', icon: '🤖', value: stats?.aiAnalyses ?? 0,                   label: 'AI Analyses' },
  ];

  return (
    <section className="page-section active" id="page-dashboard">
      <div className="page-header">
        <div>
          <h1>
            <span>🖥️</span> RESTCONF Network Operations Console
          </h1>
          <p className="subtitle">Real-time YANG/RESTCONF telemetry, Java controller status, and AI analysis</p>
        </div>
        <div className="page-header-actions">
          <button className="btn btn-outline btn-sm" onClick={load} disabled={loading}>
            {loading ? <><span className="spinner" /> Connecting to RESTCONF…</> : '🔄 Refresh Telemetry'}
          </button>
        </div>
      </div>

      {/* Stat Cards with Animated Counters */}
      <div className="stats-grid">
        {statCards.map((c, i) => (
          <div key={i} className={`stat-card ${c.color}`}>
            <div className="stat-icon">{c.icon}</div>
            <div className="stat-value">
              {loading ? <span className="skeleton skeleton-title" style={{ display: 'inline-block', width: '60px' }} /> : <AnimatedCounter value={c.value} />}
            </div>
            <div className="stat-label">{c.label}</div>
          </div>
        ))}
      </div>

      {/* Network Topology Map */}
      <div style={{ marginBottom: '24px' }}>
        <NetworkTopology devices={devices} onNavigate={navigateTo} />
      </div>

      <div className="grid-2">
        {/* Recent Operations */}
        <div className="card">
          <div className="card-header">
            <h3>⚡ Recent RESTCONF Operations</h3>
            <span className="badge badge-info">{ops.length} Captured</span>
          </div>
          <div className="card-body" id="recent-operations">
            {loading ? (
              <div>
                <div className="skeleton skeleton-text" />
                <div className="skeleton skeleton-text" />
                <div className="skeleton skeleton-text" />
              </div>
            ) : !ops.length ? (
              <div className="empty-state">
                <div className="empty-icon">📡</div>
                <p>No RESTCONF operations recorded yet. Execute CRUD requests to populate.</p>
              </div>
            ) : ops.map((op, i) => (
              <div key={i} className="history-item">
                <span className="history-time">{formatTime(op.timestamp)}</span>
                <span className={`method-badge ${op.method?.toLowerCase()}`}>{op.method}</span>
                <div className="history-detail">
                  <span className="device-name">{op.deviceName || 'Router-01'}</span>
                  <span className="text-muted text-mono" style={{ fontSize: '0.75rem', marginLeft: 6 }}>{op.resourcePath || ''}</span>
                </div>
                <span className={`badge ${op.success ? 'badge-success' : 'badge-danger'}`}>{op.statusCode}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Devices Overview */}
        <div className="card">
          <div className="card-header">
            <h3>🖥️ Registered Network Devices</h3>
            <button className="btn btn-ghost btn-sm" onClick={() => navigateTo('devices')}>Manage All →</button>
          </div>
          <div className="card-body" id="devices-overview">
            {loading ? (
              <div>
                <div className="skeleton skeleton-text" />
                <div className="skeleton skeleton-text" />
              </div>
            ) : !devices.length ? (
              <div className="empty-state">
                <div className="empty-icon">🖥️</div>
                <p>No devices registered yet.</p>
              </div>
            ) : devices.map(d => (
              <div key={d.id} className="history-item" style={{ cursor: 'pointer' }} onClick={() => navigateTo('devices')}>
                <span className={`status-dot ${d.status === 'online' || d.status === 'running' ? 'online' : 'offline'}`} />
                <div className="history-detail">
                  <span className="device-name">{d.name}</span>
                  <span className="text-muted text-mono" style={{ fontSize: '0.75rem', marginLeft: 8 }}>{d.ipAddress} • {d.vendor || ''} {d.model || ''}</span>
                </div>
                <span className={`badge ${d.status === 'online' || d.status === 'running' ? 'badge-success' : 'badge-warning'}`}>{d.status || 'running'}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
