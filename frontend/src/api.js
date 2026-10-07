/**
 * api.js — Centralised fetch helpers for the RESTCONF AI backend.
 * All paths are relative so the Vite dev proxy handles them in dev mode.
 */

export const API = {
  devices:   '/api/devices',
  restconf:  '/api/restconf',
  ai:        '/api/ai',
  dashboard: '/api/devices/dashboard/stats',
  history:   '/api/devices/history',
  simulator: '/restconf/operations/reset',
};

async function handleResponse(res) {
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    let msg = data.message || data.error || `HTTP ${res.status}`;
    if (data.fieldErrors && typeof data.fieldErrors === 'object') {
      const details = Object.entries(data.fieldErrors).map(([f, err]) => `${f}: ${err}`).join(', ');
      if (details) msg += ` (${details})`;
    }
    throw new Error(msg);
  }
  return data;
}

export const apiGet    = (url)        => fetch(url).then(handleResponse);
export const apiPost   = (url, body)  => fetch(url, { method: 'POST',  headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }).then(handleResponse);
export const apiPut    = (url, body)  => fetch(url, { method: 'PUT',   headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }).then(handleResponse);
export const apiPatch  = (url, body)  => fetch(url, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }).then(handleResponse);
export const apiDelete = (url)        => fetch(url, { method: 'DELETE' }).then(r => r.json().catch(() => ({})));
