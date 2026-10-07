import React, { useState } from 'react';

export default function NetworkTopology({ devices = [], onSelectDevice, onNavigate }) {
  const [hoveredNode, setHoveredNode] = useState(null);

  return (
    <div className="card">
      <div className="card-header">
        <h3>🌐 Interactive Network Topology</h3>
        <span className="badge badge-info">RESTCONF Active</span>
      </div>
      <div className="card-body" style={{ background: '#090D16', padding: '24px', position: 'relative' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '16px', position: 'relative' }}>
          
          {/* Node 1: Java Application Client */}
          <div 
            className="flow-node active"
            style={{ borderColor: '#2563EB', background: 'rgba(37, 99, 235, 0.1)' }}
            onMouseEnter={() => setHoveredNode('Java Client')}
            onMouseLeave={() => setHoveredNode(null)}
          >
            <div className="flow-node-icon">☕</div>
            <div className="flow-node-title" style={{ color: '#60A5FA' }}>Java App</div>
            <div className="flow-node-sub">Spring Boot Engine</div>
          </div>

          {/* Line 1: Java -> RESTCONF */}
          <div className="flow-line active" />

          {/* Node 2: RESTCONF API Layer */}
          <div 
            className="flow-node active"
            style={{ borderColor: '#06B6D4', background: 'rgba(6, 182, 212, 0.1)' }}
            onMouseEnter={() => setHoveredNode('RESTCONF API')}
            onMouseLeave={() => setHoveredNode(null)}
          >
            <div className="flow-node-icon">⚡</div>
            <div className="flow-node-title" style={{ color: '#22D3EE' }}>RESTCONF Gateway</div>
            <div className="flow-node-sub">YANG Protocol Layer</div>
          </div>

          {/* Line 2: RESTCONF -> Devices */}
          <div className="flow-line active" />

          {/* Node Group 3: Network Devices */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {devices.length === 0 ? (
              <div className="flow-node">
                <div className="flow-node-icon">🖥️</div>
                <div className="flow-node-title">No Devices</div>
              </div>
            ) : (
              devices.map((device) => (
                <div 
                  key={device.id}
                  className="flow-node"
                  style={{ 
                    cursor: 'pointer', 
                    borderColor: device.status === 'running' || device.status === 'online' ? '#22C55E' : '#F59E0B',
                    background: 'rgba(15, 23, 42, 0.9)'
                  }}
                  onClick={() => {
                    if (onSelectDevice) onSelectDevice(device.id);
                    if (onNavigate) onNavigate('crud');
                  }}
                  onMouseEnter={() => setHoveredNode(device.name)}
                  onMouseLeave={() => setHoveredNode(null)}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className={`status-dot ${device.status === 'running' || device.status === 'online' ? 'online' : 'offline'}`} />
                    <span className="flow-node-title">{device.name}</span>
                  </div>
                  <div className="flow-node-sub">{device.ipAddress} • {device.model || 'CSR1000V'}</div>
                </div>
              ))
            )}
          </div>
        </div>

        {hoveredNode && (
          <div style={{ 
            marginTop: '16px', 
            padding: '8px 12px', 
            background: '#0F172A', 
            borderRadius: '6px', 
            border: '1px solid #1E293B',
            fontSize: '0.78rem',
            fontFamily: "'JetBrains Mono', monospace",
            color: '#9CA3AF'
          }}>
            Status: <span style={{ color: '#22D3EE' }}>Active Link</span> • Target: <span style={{ color: '#E5E7EB' }}>{hoveredNode}</span>
          </div>
        )}
      </div>
    </div>
  );
}
