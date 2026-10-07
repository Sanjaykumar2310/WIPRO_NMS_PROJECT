import React, { useState, useEffect } from 'react';
import { API, apiGet, apiPost } from '../api.js';
import { formatMarkdown } from '../utils.js';

const ANALYSIS_TYPES = [
  { type: 'analyze',      icon: '🔍', label: 'Analyze Config' },
  { type: 'explain',      icon: '💡', label: 'Explain YANG' },
  { type: 'issues',       icon: '⚠️', label: 'Detect Issues' },
  { type: 'recommend',    icon: '📋', label: 'Recommendations' },
  { type: 'troubleshoot', icon: '🔧', label: 'Troubleshoot' },
  { type: 'summarize',    icon: '📝', label: 'Summary' },
];

const PIPELINE_STEPS = [
  { id: 1, label: 'Reading RESTCONF data' },
  { id: 2, label: 'Checking interfaces' },
  { id: 3, label: 'Detecting issues' },
  { id: 4, label: 'Generating recommendations' },
  { id: 5, label: 'Analysis Complete' },
];

export default function AIAnalysis({ devices, showToast, prefillData }) {
  const [deviceId, setDeviceId]   = useState('');
  const [configData, setConfigData] = useState('');
  const [question, setQuestion]   = useState('');
  const [result, setResult]       = useState(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [activeStep, setActiveStep] = useState(0);
  const [aiStatus, setAiStatus]   = useState(null);

  useEffect(() => {
    apiGet(`${API.ai}/status`).then(setAiStatus).catch(() => {});
  }, []);

  useEffect(() => {
    if (prefillData) {
      setConfigData(prefillData.body || '');
      if (prefillData.deviceId) setDeviceId(prefillData.deviceId);
    }
  }, [prefillData]);

  async function loadDeviceData(did) {
    if (!did) return;
    try {
      const response = await apiGet(`${API.restconf}/device/${did}/config`);
      setConfigData(response.body || JSON.stringify(response, null, 2));
    } catch (e) {
      showToast('error', 'Error', 'Failed to load device RESTCONF config: ' + e.message);
    }
  }

  function handleDeviceChange(e) {
    const did = e.target.value;
    setDeviceId(did);
    loadDeviceData(did);
  }

  async function runAnalysis(type) {
    if (!configData.trim()) {
      showToast('warning', 'Required', 'Load or paste configuration data first.');
      return;
    }
    const deviceName = devices.find(d => d.id === deviceId)?.name || 'Router-01';
    setAnalyzing(true);
    setResult(null);
    setActiveStep(1);

    // Simulate step progression
    const t1 = setTimeout(() => setActiveStep(2), 400);
    const t2 = setTimeout(() => setActiveStep(3), 800);
    const t3 = setTimeout(() => setActiveStep(4), 1200);

    try {
      const payload = { deviceName, configData };
      if (type === 'troubleshoot') payload.question = question || 'What are the potential issues?';
      const r = await apiPost(`${API.ai}/${type}`, payload);
      setResult(r);
      setActiveStep(5);
      showToast('success', 'AI Analysis Complete', `${type} analysis finished in ${r.processingTimeMs}ms`);
    } catch (e) {
      showToast('error', 'AI Error', e.message);
      setResult({ analysis: 'AI analysis failed: ' + e.message });
      setActiveStep(0);
    } finally {
      clearTimeout(t1); clearTimeout(t2); clearTimeout(t3);
      setAnalyzing(false);
    }
  }

  return (
    <section className="page-section active" id="page-ai-analysis">
      <div className="page-header">
        <div>
          <h1><span>🤖</span> AI Network Assistant</h1>
          <p className="subtitle">Intelligent RESTCONF configuration analysis, issue detection, and troubleshooting</p>
        </div>
        <div className="page-header-actions">
          {aiStatus && (
            <span className={`badge ${aiStatus.configured ? 'badge-purple' : 'badge-warning'}`} id="ai-status-badge">
              {aiStatus.configured
                ? `✓ ${(aiStatus.activeProvider || 'AI').toUpperCase()} Engine Active`
                : 'Fallback mode (no API key)'}
            </span>
          )}
        </div>
      </div>

      <div className="grid-2">
        {/* ─── Input Card ─── */}
        <div className="card">
          <div className="card-header">
            <h3>📊 Target Device & Configuration</h3>
          </div>
          <div className="card-body">
            <div className="form-group">
              <label htmlFor="ai-device">Select Device</label>
              <select id="ai-device" className="form-control" value={deviceId} onChange={handleDeviceChange}>
                <option value="">Select a device to auto-load config…</option>
                {devices.map(d => (
                  <option key={d.id} value={d.id}>{d.name} ({d.ipAddress})</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="ai-config-data">RESTCONF Configuration Data (JSON)</label>
              <textarea
                id="ai-config-data"
                className="form-control"
                rows={9}
                placeholder="Paste or auto-load device configuration data here…"
                value={configData}
                onChange={e => setConfigData(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label htmlFor="ai-question">Troubleshooting Query (Optional)</label>
              <input
                id="ai-question"
                type="text"
                className="form-control"
                placeholder="e.g., Why is GigabitEthernet3 interface down?"
                value={question}
                onChange={e => setQuestion(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label>Select Analysis Pipeline Action</label>
              <div className="crud-toolbar">
                {ANALYSIS_TYPES.map(({ type, icon, label }) => (
                  <button
                    key={type}
                    className="btn btn-outline btn-sm"
                    onClick={() => runAnalysis(type)}
                    disabled={analyzing}
                  >
                    {icon} {label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* ─── AI Result Panel ─── */}
        <div className="ai-panel" id="ai-result-panel">
          {analyzing && <div className="ai-scanning-bar" />}
          <div className="ai-header">
            <span className="ai-icon">🤖</span>
            <h3>AI Network Analysis Output</h3>
          </div>
          <div className="ai-body">
            
            {/* Visual Pipeline Bar */}
            <div className="ai-pipeline-container">
              {PIPELINE_STEPS.map((step) => {
                const isActive = activeStep === step.id;
                const isDone = activeStep > step.id || (activeStep === 5 && step.id === 5);
                return (
                  <div key={step.id} className={`pipeline-step ${isActive ? 'active' : isDone ? 'done' : ''}`}>
                    <span>{isDone ? '✓' : step.id}</span>
                    <span>{step.label}</span>
                  </div>
                );
              })}
            </div>

            <div className="ai-result" id="ai-result-content">
              {analyzing ? (
                <div className="flex-center" style={{ padding: '40px', gap: '14px', flexDirection: 'column' }}>
                  <span className="spinner spinner-lg" style={{ borderColor: 'rgba(139,92,246,0.3)', borderTopColor: '#8B5CF6' }} />
                  <span className="text-mono" style={{ color: '#A78BFA', fontSize: '0.85rem' }}>
                    AI analyzing configuration and checking RESTCONF interfaces…
                  </span>
                </div>
              ) : !result ? (
                <div className="empty-state">
                  <div className="empty-icon">🤖</div>
                  <p>Select a device configuration and run an analysis pipeline action to inspect issues.</p>
                </div>
              ) : (
                <div className="ai-markdown-body" dangerouslySetInnerHTML={{ __html: formatMarkdown(result.analysis || 'No analysis returned.') }} />
              )}
            </div>

            {result && !analyzing && (
              <div className="ai-meta" id="ai-result-meta">
                <span>🏷️ Type: <span id="ai-result-type" style={{ color: '#22D3EE' }}>{result.type || '—'}</span></span>
                <span>🤖 Engine: <span id="ai-result-provider" style={{ color: '#A78BFA' }}>{result.aiProvider || 'HuggingFace'}</span></span>
                <span>⏱️ Latency: <span id="ai-result-time" style={{ color: '#FBBF24' }}>{result.processingTimeMs ? `${result.processingTimeMs}ms` : '—'}</span></span>
                {result.severity && (
                  <span>⚡ Severity: <span
                    id="ai-result-severity"
                    className={result.severity === 'critical' ? 'text-danger' : result.severity === 'warning' ? 'text-warning' : 'text-success'}
                  >{result.severity}</span></span>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
