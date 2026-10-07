import React, { useState, useEffect, useCallback } from 'react';
import Sidebar from './components/Sidebar.jsx';
import ToastContainer from './components/ToastContainer.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Devices from './pages/Devices.jsx';
import CrudOperations from './pages/CrudOperations.jsx';
import NetworkConfig from './pages/NetworkConfig.jsx';
import AIAnalysis from './pages/AIAnalysis.jsx';
import AIConfigAnalyzer from './pages/AIConfigAnalyzer.jsx';
import OperationHistory from './pages/OperationHistory.jsx';
import Settings from './pages/Settings.jsx';
import { useToast } from './useToast.js';

export default function App() {
  const { toasts, showToast, removeToast } = useToast();

  // ── Navigation ───────────────────────────────────────────
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [pageParams, setPageParams]   = useState({});
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [collapsed, setCollapsed]     = useState(false);

  const navigateTo = useCallback((page, params = {}) => {
    setCurrentPage(page);
    setPageParams(params);
    setSidebarOpen(false);
    window.scrollTo(0, 0);
  }, []);

  // ── Shared device list ───────────────────────────────────
  const [devices, setDevices] = useState([]);

  // ── Cross-page state ─────────────────────────────────────
  const [aiPrefill, setAiPrefill] = useState(null);

  function handleResponseForAI(data) {
    setAiPrefill(data);
    navigateTo('ai-analysis');
  }

  // ── Page renderer ────────────────────────────────────────
  function renderPage() {
    switch (currentPage) {
      case 'dashboard':
        return <Dashboard showToast={showToast} navigateTo={navigateTo} />;

      case 'devices':
        return (
          <Devices
            showToast={showToast}
            navigateTo={navigateTo}
            onDevicesChange={setDevices}
          />
        );

      case 'crud':
        return (
          <CrudOperations
            devices={devices}
            showToast={showToast}
            onResponseForAI={handleResponseForAI}
          />
        );

      case 'network-config':
        return (
          <NetworkConfig
            devices={devices}
            showToast={showToast}
            preSelectDevice={pageParams.preSelectDevice}
          />
        );

      case 'ai-analyzer':
        return (
          <AIConfigAnalyzer
            devices={devices}
            showToast={showToast}
          />
        );

      case 'ai-analysis':
        return (
          <AIAnalysis
            devices={devices}
            showToast={showToast}
            prefillData={aiPrefill}
          />
        );

      case 'history':
        return <OperationHistory showToast={showToast} />;

      case 'settings':
        return <Settings showToast={showToast} />;

      default:
        return <Dashboard showToast={showToast} navigateTo={navigateTo} />;
    }
  }

  return (
    <div className="app-container">
      <Sidebar
        currentPage={currentPage}
        navigateTo={navigateTo}
        deviceCount={devices.length}
        sidebarOpen={sidebarOpen}
        setSidebarOpen={setSidebarOpen}
        collapsed={collapsed}
        setCollapsed={setCollapsed}
      />

      <main className={`main-content${collapsed ? ' collapsed' : ''}`}>
        <div className="content-wrapper">
          {/* Mobile hamburger */}
          <button
            className="btn btn-ghost mobile-toggle"
            onClick={() => setSidebarOpen(o => !o)}
            aria-label="Toggle menu"
          >
            ☰ Menu
          </button>

          {/* Active page */}
          {renderPage()}
        </div>
      </main>

      <ToastContainer toasts={toasts} removeToast={removeToast} />
    </div>
  );
}
