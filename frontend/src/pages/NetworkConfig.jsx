import React, { useState, useEffect, useCallback } from 'react';
import { API, apiGet } from '../api.js';
import { safeHighlightJson, formatUptime } from '../utils.js';

export default function NetworkConfig({ devices, showToast, preSelectDevice }) {
  const [deviceId, setDeviceId] = useState(preSelectDevice || '');
  const [config, setConfig]     = useState(null);
  const [raw, setRaw]           = useState(null);
  const [loading, setLoading]   = useState(false);

  const load = useCallback(async (did) => {
    if (!did) { setConfig(null); setRaw(null); return; }
    setLoading(true);
    try {
      const response = await apiGet(`${API.restconf}/device/${did}/config`);
      let parsed;
      try { parsed = JSON.parse(response.body); } catch { parsed = response; }
      const device = parsed['network-device:device'] || parsed;
      setConfig(device);
      setRaw(response.body || JSON.stringify(response));
    } catch (e) {
      showToast('error', 'Error', 'Failed to load config: ' + e.message);
      setConfig(null);
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    if (preSelectDevice) { setDeviceId(preSelectDevice); load(preSelectDevice); }
  }, [preSelectDevice, load]);

  function handleSelect(e) {
    const id = e.target.value;
    setDeviceId(id);
    load(id);
  }

  function copyRaw() {
    if (raw) { navigator.clipboard.writeText(raw); showToast('info', 'Copied', 'JSON payload copied to clipboard.'); }
  }

  const interfaces = config?.interfaces || [];

  return (
    <section className="page-section active" id="page-network-config">
      <div className="page-header">
        <div>
          <h1><span>🔧</span> RESTCONF Network Device Configuration</h1>
          <p className="subtitle">Inspect YANG data models, running device configurations, and interface status</p>
        </div>
      </div>

      <div className="form-group mb-2">
        <label htmlFor="config-device-select">Target Device</label>
        <select id="config-device-select" className="form-control" value={deviceId} onChange={handleSelect} style={{ maxWidth: 420 }}>
          <option value="">Select a device to inspect YANG configuration…</option>
          {devices.map(d => (
            <option key={d.id} value={d.id}>{d.name} ({d.ipAddress})</option>
          ))}
        </select>
      </div>

      <div id="network-config-content">
        {loading ? (
          <div className="flex-center mt-3" style={{ padding: '60px', gap: '14px', flexDirection: 'column' }}>
            <span className="spinner spinner-lg" style={{ borderColor: 'rgba(6,182,212,0.3)', borderTopColor: '#06B6D4' }} />
            <span className="text-mono" style={{ color: '#22D3EE', fontSize: '0.85rem' }}>
              Fetching RESTCONF device configuration payload…
            </span>
          </div>
        ) : !config ? (
          <div className="empty-state mt-3">
            <div className="empty-icon">🔧</div>
            <p>Select a device above to retrieve its RESTCONF YANG configuration payload.</p>
          </div>
        ) : (
          <>
            <div className="grid-2 mt-2">
              {/* Device Info */}
              <div className="card">
                <div className="card-header">
                  <h3>📟 Device System Information</h3>
                  <span className="badge badge-success">{config.status || 'running'}</span>
                </div>
                <div className="card-body">
                  <table>
                    <tbody>
                      {[
                        ['Hostname',      config.hostname],
                        ['Vendor',        config.vendor],
                        ['Model',         config.model],
                        ['OS Version',    config.osVersion],
                        ['Serial Number', config.serialNumber],
                        ['Uptime',        formatUptime(config.uptime)],
                      ].map(([k, v]) => (
                        <tr key={k}>
                          <td style={{ width: '40%', fontWeight: 600 }} className="text-mono">{k}</td>
                          <td className="text-mono" style={{ color: '#22D3EE' }}>{v || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Interfaces */}
              <div className="card">
                <div className="card-header">
                  <h3>🔌 Interfaces ({interfaces.length})</h3>
                  <span className="badge badge-info">YANG List</span>
                </div>
                <div className="card-body">
                  <div className="table-container">
                    <table>
                      <thead>
                        <tr><th>Name</th><th>IP Address</th><th>Status</th><th>Description</th></tr>
                      </thead>
                      <tbody>
                        {interfaces.map((iface, i) => (
                          <tr key={i}>
                            <td className="text-mono" style={{ fontWeight: 600, color: '#E5E7EB' }}>{iface.name}</td>
                            <td className="text-mono" style={{ color: '#A78BFA' }}>{iface.ipAddress || '—'}</td>
                            <td>
                              <span className={`badge ${iface.enabled && iface.status === 'up' ? 'badge-success' : 'badge-danger'}`}>
                                {iface.enabled ? '✓' : '✗'} {iface.status || 'unknown'}
                              </span>
                            </td>
                            <td style={{ fontSize: '0.8rem' }}>{iface.description || '—'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>

            {/* Terminal Style Raw JSON */}
            <div className="terminal-window mt-2">
              <div className="terminal-header">
                <div className="terminal-dots">
                  <div className="dot red" />
                  <div className="dot yellow" />
                  <div className="dot green" />
                </div>
                <span className="terminal-title">RESTCONF JSON Output — GET /restconf/data/network-device:device</span>
                <button className="btn btn-ghost btn-sm" onClick={copyRaw}>📋 Copy JSON</button>
              </div>
              <div className="terminal-body">
                <div
                  className="json-viewer"
                  id="raw-config-json"
                  style={{ border: 'none' }}
                  dangerouslySetInnerHTML={{ __html: safeHighlightJson(raw || '{}') }}
                />
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  );
}
