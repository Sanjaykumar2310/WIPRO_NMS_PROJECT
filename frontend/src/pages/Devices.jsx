import React, { useEffect, useState, useCallback } from 'react';
import { API, apiGet, apiPost, apiDelete } from '../api.js';

function AddDeviceModal({ onClose, onAdded, showToast }) {
  const [form, setForm] = useState({
    name: '', ip: '', url: 'http://localhost:8080/restconf',
    vendor: '', model: '', username: 'admin', password: 'admin',
  });
  const [saving, setSaving] = useState(false);

  const set = (k, v) => setForm(p => ({ ...p, [k]: v }));

  async function handleAdd() {
    if (!form.name.trim() || !form.ip.trim()) {
      showToast('warning', 'Validation', 'Device name and IP address are required.');
      return;
    }
    setSaving(true);
    try {
      await apiPost(API.devices, {
        name: form.name.trim(), ipAddress: form.ip.trim(),
        restconfUrl: form.url.trim(), vendor: form.vendor.trim(),
        model: form.model.trim(), username: form.username.trim(),
        password: form.password.trim(),
      });
      showToast('success', 'Device Added', `${form.name} registered successfully.`);
      onAdded();
      onClose();
    } catch (e) {
      showToast('error', 'Error', 'Failed to add device: ' + e.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-overlay active" id="add-device-modal">
      <div className="modal">
        <div className="modal-header">
          <h2>Add New Device</h2>
          <button className="btn btn-ghost" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          {[
            ['Device Name *', 'name',     'text',     'e.g., Router-02'],
            ['IP Address *',  'ip',       'text',     'e.g., 192.168.1.10'],
            ['RESTCONF URL',  'url',      'text',     'http://localhost:8080/restconf'],
            ['Vendor',        'vendor',   'text',     'e.g., Cisco'],
            ['Model',         'model',    'text',     'e.g., CSR1000V'],
          ].map(([label, key, type, placeholder]) => (
            <div className="form-group" key={key}>
              <label htmlFor={`new-device-${key}`}>{label}</label>
              <input
                id={`new-device-${key}`}
                type={type}
                className="form-control"
                placeholder={placeholder}
                value={form[key]}
                onChange={e => set(key, e.target.value)}
              />
            </div>
          ))}
          <div className="grid-2">
            <div className="form-group">
              <label htmlFor="new-device-username">Username</label>
              <input id="new-device-username" type="text" className="form-control" value={form.username} onChange={e => set('username', e.target.value)} />
            </div>
            <div className="form-group">
              <label htmlFor="new-device-password">Password</label>
              <input id="new-device-password" type="password" className="form-control" value={form.password} onChange={e => set('password', e.target.value)} />
            </div>
          </div>
        </div>
        <div className="modal-footer">
          <button className="btn btn-outline" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={handleAdd} disabled={saving}>
            {saving ? <><span className="spinner" /> Saving…</> : 'Add Device'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function Devices({ showToast, navigateTo, onDevicesChange }) {
  const [devices, setDevices]   = useState([]);
  const [loading, setLoading]   = useState(true);
  const [showModal, setShowModal] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiGet(API.devices);
      setDevices(data);
      onDevicesChange(data);
    } catch (e) {
      showToast('error', 'Error', 'Failed to load devices: ' + e.message);
    } finally {
      setLoading(false);
    }
  }, [showToast, onDevicesChange]);

  useEffect(() => { load(); }, [load]);

  async function handleDelete(id) {
    if (!window.confirm('Are you sure you want to delete this device?')) return;
    try {
      await apiDelete(`${API.devices}/${id}`);
      showToast('success', 'Deleted', 'Device removed.');
      load();
    } catch (e) {
      showToast('error', 'Error', 'Failed to delete device: ' + e.message);
    }
  }

  function viewConfig(id) {
    navigateTo('network-config', { preSelectDevice: id });
  }

  return (
    <section className="page-section active" id="page-devices">
      <div className="page-header">
        <div>
          <h1>Device Management</h1>
          <p className="subtitle">Manage RESTCONF-enabled network devices</p>
        </div>
        <div className="page-header-actions">
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Add Device</button>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="table-container">
            <table id="devices-table">
              <thead>
                <tr>
                  <th>Status</th>
                  <th>Device Name</th>
                  <th>IP Address</th>
                  <th>Vendor / Model</th>
                  <th>RESTCONF URL</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody id="devices-table-body">
                {loading ? (
                  <tr><td colSpan={6}><div className="flex-center" style={{ padding: '30px' }}><span className="spinner spinner-lg" /></div></td></tr>
                ) : !devices.length ? (
                  <tr><td colSpan={6}><div className="empty-state"><p>No devices found. Click "Add Device" to register one.</p></div></td></tr>
                ) : devices.map(d => (
                  <tr key={d.id}>
                    <td><span className={`status-dot ${d.status === 'online' ? 'online' : d.status === 'offline' ? 'offline' : 'unknown'}`} /></td>
                    <td><strong>{d.name}</strong></td>
                    <td className="text-mono">{d.ipAddress}</td>
                    <td>{d.vendor || '—'} / {d.model || '—'}</td>
                    <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>{d.restconfUrl || '—'}</td>
                    <td>
                      <button className="btn btn-ghost btn-sm" onClick={() => viewConfig(d.id)} title="View Config">🔧</button>
                      <button className="btn btn-ghost btn-sm" onClick={() => handleDelete(d.id)} title="Delete" style={{ color: 'var(--accent-danger)' }}>🗑️</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {showModal && (
        <AddDeviceModal
          onClose={() => setShowModal(false)}
          onAdded={load}
          showToast={showToast}
        />
      )}
    </section>
  );
}
