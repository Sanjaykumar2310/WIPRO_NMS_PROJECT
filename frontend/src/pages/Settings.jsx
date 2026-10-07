import React, { useEffect, useState, useCallback } from 'react';
import { API, apiGet, apiPost } from '../api.js';

export default function Settings({ showToast }) {
  const [aiStatus, setAiStatus] = useState(null);
  const [hfKey, setHfKey]       = useState('');
  const [saving, setSaving]     = useState(false);
  const [testing, setTesting]   = useState(false);
  const [testResult, setTestResult] = useState(null);
  const [resetting, setResetting] = useState(false);

  const loadStatus = useCallback(async () => {
    try {
      const s = await apiGet(`${API.ai}/status`);
      setAiStatus(s);
      setHfKey(s.huggingfaceApiKeyMasked || '');
    } catch (e) {
      console.error('Failed to load AI status:', e);
    }
  }, []);

  useEffect(() => { loadStatus(); }, [loadStatus]);

  async function saveAISettings() {
    setSaving(true);
    try {
      await apiPost(`${API.ai}/settings`, {
        provider: 'huggingface',
        geminiApiKey: '', geminiModel: 'gemini-1.5-flash',
        openaiApiKey: '', openaiModel: 'gpt-3.5-turbo',
        huggingfaceApiKey: hfKey,
      });
      showToast('success', 'Saved', 'Hugging Face API key updated & saved to .env file.');
      await loadStatus();
    } catch (e) {
      showToast('error', 'Error', 'Failed to save settings: ' + e.message);
    } finally {
      setSaving(false);
    }
  }

  async function testAIConnection() {
    setTesting(true);
    setTestResult(null);
    try {
      const res = await apiPost(`${API.ai}/test`, {});
      setTestResult(res);
      if (res.success) showToast('success', 'AI Connected', 'AI API is responding correctly!');
      else showToast('error', 'Connection Failed', res.message);
    } catch (e) {
      setTestResult({ success: false, message: e.message });
      showToast('error', 'Test Failed', e.message);
    } finally {
      setTesting(false);
    }
  }

  async function resetSimulator() {
    if (!window.confirm('Reset simulator data to initial state? This will undo all changes.')) return;
    setResetting(true);
    try {
      await apiPost(API.simulator, {});
      showToast('success', 'Reset', 'Simulator data restored to initial state.');
    } catch (e) {
      showToast('error', 'Error', 'Failed to reset simulator: ' + e.message);
    } finally {
      setResetting(false);
    }
  }

  return (
    <section className="page-section active" id="page-settings">
      <div className="page-header">
        <div>
          <h1>Settings</h1>
          <p className="subtitle">Application configuration and status</p>
        </div>
      </div>

      <div className="grid-2">
        {/* RESTCONF Config */}
        <div className="card">
          <div className="card-header"><h3>🔌 RESTCONF Configuration</h3></div>
          <div className="card-body">
            <div className="form-group">
              <label>Mode</label>
              <div><span className="badge badge-success">Simulator</span></div>
            </div>
            <div className="form-group">
              <label>Simulator URL</label>
              <input type="text" className="form-control" value="http://localhost:8080/restconf" disabled />
            </div>
            <div className="form-group">
              <label>RESTCONF Standard</label>
              <input type="text" className="form-control" value="RFC 8040" disabled />
            </div>
            <div className="form-group mt-2">
              <button className="btn btn-warning btn-sm" onClick={resetSimulator} disabled={resetting}>
                {resetting ? <><span className="spinner" /> Resetting…</> : '🔄 Reset Simulator Data'}
              </button>
            </div>
          </div>
        </div>

        {/* AI Config */}
        <div className="card">
          <div className="card-header"><h3>🤖 AI Configuration</h3></div>
          <div className="card-body" id="ai-settings-content">
            {!aiStatus ? (
              <p className="text-muted" style={{ fontSize: '0.85rem' }}>Loading AI status…</p>
            ) : (
              <>
                <div className="form-group mb-2">
                  <label style={{ fontWeight: 600 }}>Active AI Provider</label>
                  <div style={{ marginTop: 4 }}>
                    <span className={`badge ${aiStatus.configured ? 'badge-success' : 'badge-warning'}`} style={{ fontSize: '0.9rem', padding: '6px 12px' }}>
                      {aiStatus.configured
                        ? (aiStatus.activeProvider ? aiStatus.activeProvider.toUpperCase() : 'HUGGINGFACE')
                        : 'Not Configured (Rule-Based Fallback Mode)'}
                    </span>
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="setting-huggingface-key">Hugging Face API Key</label>
                  <input
                    id="setting-huggingface-key"
                    type="password"
                    className="form-control"
                    placeholder="Enter Hugging Face API key (hf_…)"
                    value={hfKey}
                    onChange={e => setHfKey(e.target.value)}
                  />
                  <small className="text-muted" style={{ display: 'block', marginTop: 6 }}>
                    Model: <code>{aiStatus.huggingfaceModel || 'Qwen/Qwen2.5-Coder-32B-Instruct'}</code>
                  </small>
                </div>

                <div className="flex gap-2 mt-3" style={{ display: 'flex', gap: 10 }}>
                  <button className="btn btn-primary" onClick={saveAISettings} disabled={saving}>
                    {saving ? <><span className="spinner" /> Saving…</> : '💾 Save AI Settings'}
                  </button>
                  <button className="btn btn-outline" onClick={testAIConnection} disabled={testing}>
                    {testing ? <><span className="spinner" /> Testing…</> : '🧪 Test AI Connection'}
                  </button>
                </div>

                {testResult && (
                  <div id="ai-test-result" className="mt-2">
                    <div
                      className={`badge ${testResult.success ? 'badge-success' : 'badge-danger'}`}
                      style={{ whiteSpace: 'normal', textAlign: 'left', fontSize: '0.85rem', lineHeight: 1.4, padding: '8px 12px', display: 'block' }}
                    >
                      {testResult.success ? '✅' : '❌'} {testResult.message}
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      {/* About */}
      <div className="card mt-2">
        <div className="card-header"><h3>📘 About This Project</h3></div>
        <div className="card-body">
          <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', lineHeight: 1.8 }}>
            <strong>CRUD Operations with RESTCONF and Java with AI Integration</strong><br /><br />
            This application demonstrates how a Java Spring Boot backend communicates with RESTCONF-enabled network
            devices to perform CRUD operations, and integrates AI for intelligent configuration analysis and
            troubleshooting recommendations.<br /><br />
            <strong>Architecture:</strong> User → React Frontend → Java Spring Boot → CRUD Service → RESTCONF Client → RESTCONF Server → Network Device<br />
            <strong>AI Flow:</strong> RESTCONF Response → Java Backend → AI API → Analysis → User<br /><br />
            <strong>Technologies:</strong> Java 17, Spring Boot, React.js, Vite, RESTCONF (RFC 8040), YANG, JSON, AI API (Hugging Face)
          </p>
        </div>
      </div>
    </section>
  );
}
