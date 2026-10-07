import React, { useState, useEffect, useCallback } from 'react';
import { API, apiGet, apiPost, apiPut, apiPatch } from '../api.js';
import { safeHighlightJson } from '../utils.js';

const METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
const METHOD_ENDPOINTS = {
  GET:    `${API.restconf}/read`,
  POST:   `${API.restconf}/create`,
  PUT:    `${API.restconf}/update`,
  PATCH:  `${API.restconf}/update`,
  DELETE: `${API.restconf}/delete`,
};

export default function CrudOperations({ devices, showToast, onResponseForAI }) {
  const [method, setMethod]           = useState('GET');
  const [deviceId, setDeviceId]       = useState('');
  const [resourcePath, setResourcePath] = useState('');
  const [resources, setResources]     = useState([]);
  const [body, setBody]               = useState('');
  const [originalBody, setOriginalBody] = useState('');
  const [response, setResponse]       = useState(null);
  const [executing, setExecuting]     = useState(false);
  const [requestState, setRequestState] = useState('idle'); // idle | sending | processing | complete
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showDiff, setShowDiff]       = useState(false);

  const needsBody = ['POST', 'PUT', 'PATCH'].includes(method);
  const targetDevice = devices.find(d => d.id === deviceId);

  async function loadResources(did) {
    setResources([]);
    setResourcePath('');
    if (!did) return;
    try {
      const paths = await apiGet(`${API.restconf}/device/${did}/resources`);
      setResources(paths);
    } catch { /* ignore */ }
  }

  useEffect(() => { loadResources(deviceId); }, [deviceId]);

  // Load current device config as original body when resource changes
  useEffect(() => {
    if (deviceId && resourcePath && (method === 'PUT' || method === 'PATCH')) {
      apiGet(`${API.restconf}/device/${deviceId}/config`).then(r => {
        setOriginalBody(r.body || '');
      }).catch(() => {});
    }
  }, [deviceId, resourcePath, method]);

  async function handleExecute() {
    if (!deviceId)     { showToast('warning', 'Required', 'Please select a target device.'); return; }
    if (!resourcePath) { showToast('warning', 'Required', 'Please select a RESTCONF resource path.'); return; }
    if (needsBody && !body.trim()) {
      showToast('warning', 'Required', `Request body required for ${method} operations.`);
      return;
    }

    if (method === 'DELETE') {
      setShowDeleteModal(true);
      return;
    }

    performApiCall();
  }

  async function performApiCall() {
    setExecuting(true);
    setRequestState('sending');
    setResponse(null);

    const timer1 = setTimeout(() => setRequestState('processing'), 300);

    try {
      const payload = { deviceId, resourcePath };
      if (body.trim()) payload.body = body.trim();

      const endpoint = METHOD_ENDPOINTS[method];
      let result;
      if (method === 'PATCH') result = await apiPatch(endpoint, payload);
      else if (method === 'PUT') result = await apiPut(endpoint, payload);
      else result = await apiPost(endpoint, payload);

      setResponse(result);
      setRequestState('complete');
      showToast('success', 'RESTCONF Request Succeeded', `${method} ${resourcePath} → ${result.statusCode}`);
    } catch (e) {
      setResponse({ statusCode: 'Error', statusMessage: e.message, body: null, success: false });
      setRequestState('complete');
      showToast('error', 'RESTCONF Operation Failed', e.message);
    } finally {
      clearTimeout(timer1);
      setExecuting(false);
      setShowDeleteModal(false);
    }
  }

  function copyResponse() {
    if (response?.body) {
      navigator.clipboard.writeText(response.body);
      showToast('info', 'Copied', 'RESTCONF JSON response copied to clipboard.');
    }
  }

  function sendToAI() {
    if (response?.body) onResponseForAI({ body: response.body, deviceId });
  }

  const renderedBody = useCallback(() => {
    if (!response?.body) return null;
    return { __html: safeHighlightJson(response.body) };
  }, [response]);

  return (
    <section className="page-section active" id="page-crud">
      <div className="page-header">
        <div>
          <h1><span>⚡</span> RESTCONF Protocol Operations</h1>
          <p className="subtitle">Execute YANG-modeled RESTCONF requests (GET, POST, PUT, PATCH, DELETE)</p>
        </div>
      </div>

      {/* ─── RESTCONF Data Flow Visualizer ─── */}
      <div className="restconf-flow-container">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <span style={{ fontSize: '0.75rem', fontFamily: "'JetBrains Mono', monospace", color: '#9CA3AF', fontWeight: 600 }}>
            📡 RESTCONF PROTOCOL DATA FLOW TRACKER
          </span>
          <span className="badge badge-info">
            {requestState === 'idle' ? 'Ready' : requestState === 'sending' ? 'Sending Request…' : requestState === 'processing' ? 'RESTCONF Processing…' : 'Completed'}
          </span>
        </div>
        <div className="flow-nodes-wrapper">
          <div className={`flow-node ${requestState !== 'idle' ? 'active' : ''}`}>
            <div className="flow-node-icon">☕</div>
            <div className="flow-node-title">Java Client</div>
            <div className="flow-node-sub">Spring Boot Controller</div>
          </div>
          <div className={`flow-line ${requestState === 'sending' || requestState === 'processing' ? 'active' : ''}`} />
          <div className={`flow-node ${requestState === 'processing' || requestState === 'complete' ? 'active' : ''}`}>
            <div className="flow-node-icon">⚡</div>
            <div className="flow-node-title">RESTCONF API</div>
            <div className="flow-node-sub">{METHOD_ENDPOINTS[method]}</div>
          </div>
          <div className={`flow-line ${requestState === 'processing' || requestState === 'complete' ? 'active' : ''}`} />
          <div className={`flow-node ${requestState === 'complete' ? 'active' : ''}`}>
            <div className="flow-node-icon">🖥️</div>
            <div className="flow-node-title">{targetDevice?.name || 'Network Device'}</div>
            <div className="flow-node-sub">{targetDevice?.ipAddress || '192.168.1.1'}</div>
          </div>
          <div className={`flow-line ${requestState === 'complete' ? 'active' : ''}`} />
          <div className={`flow-node ${requestState === 'complete' ? 'active' : ''}`}>
            <div className="flow-node-icon">📄</div>
            <div className="flow-node-title">JSON Payload</div>
            <div className="flow-node-sub">{response ? response.statusCode : 'HTTP Status'}</div>
          </div>
        </div>
      </div>

      <div className="grid-2">
        {/* ─── Developer Console Request Builder ─── */}
        <div className="card">
          <div className="card-header">
            <h3>📝 RESTCONF Request Terminal</h3>
          </div>
          <div className="card-body">
            {/* Target Device */}
            <div className="form-group">
              <label htmlFor="crud-device">Target Device</label>
              <select id="crud-device" className="form-control" value={deviceId} onChange={e => setDeviceId(e.target.value)}>
                <option value="">Select a device…</option>
                {devices.map(d => (
                  <option key={d.id} value={d.id}>{d.name} ({d.ipAddress})</option>
                ))}
              </select>
            </div>

            {/* Method Toolbar */}
            <div className="form-group">
              <label>HTTP / RESTCONF Method</label>
              <div className="crud-toolbar">
                {METHODS.map(m => (
                  <button
                    key={m}
                    className={`btn btn-outline btn-sm crud-method-btn${method === m ? ' active' : ''}`}
                    onClick={() => setMethod(m)}
                  >
                    <span className={`method-badge ${m.toLowerCase()}`}>{m === 'DELETE' ? 'DEL' : m}</span>
                    {' '}{m === 'GET' ? 'Read' : m === 'POST' ? 'Create' : m === 'PUT' ? 'Update' : m === 'PATCH' ? 'Patch' : 'Delete'}
                  </button>
                ))}
              </div>
            </div>

            {/* Resource Path */}
            <div className="form-group">
              <label htmlFor="crud-resource">Resource Path (YANG Model URI)</label>
              <select id="crud-resource" className="form-control" value={resourcePath} onChange={e => setResourcePath(e.target.value)}>
                <option value="">Select RESTCONF resource path…</option>
                {resources.map((r, i) => (
                  <option key={i} value={r.path}>{r.path} — {r.description}</option>
                ))}
              </select>
            </div>

            {/* Body */}
            {needsBody && (
              <div className="form-group">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <label htmlFor="crud-body" style={{ margin: 0 }}>Request Payload (JSON)</label>
                  {(method === 'PUT' || method === 'PATCH') && originalBody && (
                    <button className="btn btn-ghost btn-sm" style={{ fontSize: '0.7rem' }} onClick={() => setShowDiff(!showDiff)}>
                      {showDiff ? 'Hide Diff' : '🔍 Compare Diff'}
                    </button>
                  )}
                </div>
                <textarea
                  id="crud-body"
                  className="form-control"
                  rows={7}
                  placeholder='{"name": "GigabitEthernet3", "ipAddress": "10.0.1.1", "enabled": true}'
                  value={body}
                  onChange={e => setBody(e.target.value)}
                />
              </div>
            )}

            {/* Side-by-side Configuration Diff Viewer */}
            {showDiff && (method === 'PUT' || method === 'PATCH') && (
              <div style={{ marginBottom: 16, background: '#090D16', padding: 12, borderRadius: 6, border: '1px solid #374151', fontSize: '0.75rem', fontFamily: "'JetBrains Mono', monospace" }}>
                <div style={{ color: '#F59E0B', fontWeight: 700, marginBottom: 4 }}>CONFIG DIFFERENCE INSPECION:</div>
                <div style={{ color: '#EF4444' }}>- Previous: {originalBody.substring(0, 120)}…</div>
                <div style={{ color: '#22C55E', marginTop: 2 }}>+ Modified: {body.substring(0, 120)}…</div>
              </div>
            )}

            <button
              id="crud-execute-btn"
              className="btn btn-restconf btn-lg w-full"
              onClick={handleExecute}
              disabled={executing}
            >
              {executing ? <><span className="spinner" /> Executing RESTCONF Request…</> : `⚡ Execute ${method} Request`}
            </button>
          </div>
        </div>

        {/* ─── Developer Response Terminal ─── */}
        <div className="terminal-window">
          <div className="terminal-header">
            <div className="terminal-dots">
              <div className="dot red" />
              <div className="dot yellow" />
              <div className="dot green" />
            </div>
            <span className="terminal-title">RESTCONF Response Console</span>
            {response && (
              <span className={`badge ${response.success ? 'badge-success' : 'badge-danger'}`}>
                {response.statusCode} {response.statusMessage || ''}
              </span>
            )}
          </div>
          <div className="terminal-body">
            <div className="json-viewer" id="crud-response" style={{ border: 'none' }}>
              {executing ? (
                <div className="flex-center" style={{ padding: '40px', gap: '12px', flexDirection: 'column' }}>
                  <span className="spinner spinner-lg" style={{ borderColor: 'rgba(6,182,212,0.3)', borderTopColor: '#06B6D4' }} />
                  <span style={{ color: '#22D3EE', fontSize: '0.85rem' }}>
                    Connecting to RESTCONF endpoint and fetching payload…
                  </span>
                </div>
              ) : !response ? (
                <div className="empty-state">
                  <div className="empty-icon">📡</div>
                  <p>Execute a RESTCONF operation to view HTTP response headers & JSON body.</p>
                </div>
              ) : response.body ? (
                renderedBody()
                  ? <span dangerouslySetInnerHTML={renderedBody()} />
                  : <span>{response.body}</span>
              ) : (
                <span style={{ color: '#4ADE80' }}>
                  {response.statusCode === '204 No Content' || response.success
                    ? '✓ 204 No Content — Resource operation completed successfully.'
                    : (response.statusMessage || 'No response body.')}
                </span>
              )}
            </div>
          </div>
          {response?.body && (
            <div className="card-footer" style={{ borderTop: '1px solid #1E293B', background: '#0F172A' }}>
              <button className="btn btn-purple btn-sm" onClick={sendToAI}>🤖 Analyze with AI</button>
              <button className="btn btn-ghost btn-sm" onClick={copyResponse}>📋 Copy JSON</button>
            </div>
          )}
        </div>
      </div>

      {/* ─── DELETE Confirmation Modal ─── */}
      {showDeleteModal && (
        <div className="modal-overlay active">
          <div className="modal">
            <div className="modal-header">
              <h2>⚠️ Confirm RESTCONF Delete</h2>
              <button className="btn btn-ghost btn-sm" onClick={() => setShowDeleteModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '0.9rem', marginBottom: 12, color: '#E5E7EB' }}>
                Are you sure you want to delete the following RESTCONF resource from device <strong>{targetDevice?.name}</strong>?
              </p>
              <div style={{ background: '#090D16', padding: 12, borderRadius: 6, border: '1px solid #374151', fontFamily: "'JetBrains Mono', monospace", fontSize: '0.8rem', color: '#F87171' }}>
                DELETE {resourcePath}
              </div>
              <p style={{ fontSize: '0.78rem', color: '#9CA3AF', marginTop: 12 }}>
                This operation will send a RESTCONF DELETE request. The server will return <code style={{ color: '#22D3EE' }}>204 No Content</code> upon successful resource deletion.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn btn-outline btn-sm" onClick={() => setShowDeleteModal(false)}>Cancel</button>
              <button className="btn btn-danger btn-sm" onClick={performApiCall}>Confirm & Delete Resource</button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
