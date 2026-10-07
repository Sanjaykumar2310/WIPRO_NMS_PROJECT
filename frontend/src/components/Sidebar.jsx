import React from 'react';

const NAV_ITEMS = [
  { section: 'Overview',      items: [
    { page: 'dashboard',      icon: '📊', label: 'Dashboard' },
  ]},
  { section: 'Network',       items: [
    { page: 'devices',        icon: '🖥️', label: 'Devices',          badge: true },
    { page: 'crud',           icon: '⚡', label: 'CRUD Operations' },
    { page: 'network-config', icon: '🔧', label: 'Network Config' },
  ]},
  { section: 'Intelligence',  items: [
    { page: 'ai-analyzer',    icon: '🛡️', label: 'AI Config Analyzer' },
    { page: 'ai-analysis',    icon: '🤖', label: 'AI Quick Assistant' },
  ]},
  { section: 'Audit',         items: [
    { page: 'history',        icon: '📋', label: 'Operation History' },
    { page: 'settings',       icon: '⚙️', label: 'Settings' },
  ]},
];

export default function Sidebar({ currentPage, navigateTo, deviceCount, sidebarOpen, setSidebarOpen, collapsed, setCollapsed }) {
  function handleNav(page) {
    navigateTo(page);
    setSidebarOpen(false);
  }

  return (
    <>
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div className="sidebar-overlay" onClick={() => setSidebarOpen(false)} />
      )}

      <aside className={`sidebar${sidebarOpen ? ' open' : ''}${collapsed ? ' collapsed' : ''}`} id="sidebar">
        <div className="sidebar-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div className="sidebar-logo">
            <div className="logo-icon">🌐</div>
            {!collapsed && (
              <div className="logo-text">
                RESTCONF AI
                <span>Network Management System</span>
              </div>
            )}
          </div>
          <button 
            className="btn btn-ghost btn-sm collapse-btn" 
            onClick={() => setCollapsed(c => !c)}
            title={collapsed ? "Expand Sidebar" : "Collapse Sidebar"}
            style={{ padding: '4px 8px', fontSize: '0.8rem', color: '#9CA3AF' }}
          >
            {collapsed ? '▶' : '◀'}
          </button>
        </div>

        <nav className="sidebar-nav">
          {NAV_ITEMS.map(group => (
            <React.Fragment key={group.section}>
              {!collapsed && <div className="nav-section-label">{group.section}</div>}
              {group.items.map(item => (
                <div
                  key={item.page}
                  id={`nav-${item.page}`}
                  className={`nav-item${currentPage === item.page ? ' active' : ''}`}
                  onClick={() => handleNav(item.page)}
                  role="button"
                  tabIndex={0}
                  title={collapsed ? item.label : undefined}
                  onKeyDown={e => e.key === 'Enter' && handleNav(item.page)}
                >
                  <span className="nav-icon">{item.icon}</span>
                  {!collapsed && <span className="nav-item-text">{item.label}</span>}
                  {item.badge && !collapsed && (
                    <span className="nav-badge" id="device-count-badge">{deviceCount}</span>
                  )}
                </div>
              ))}
            </React.Fragment>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="simulator-badge" style={{ justifyContent: collapsed ? 'center' : 'flex-start' }}>
            <div className="pulse" />
            {!collapsed && <span>Simulator Active</span>}
          </div>
        </div>
      </aside>
    </>
  );
}
