/**
 * utils.js — Shared utility functions
 */

export function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = String(text);
  return div.innerHTML;
}

export function syntaxHighlight(json) {
  if (!json) return '';
  return json.replace(
    /("(\\u[\da-fA-F]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
    (match) => {
      let cls = 'json-number';
      if (/^"/.test(match)) {
        cls = /:$/.test(match) ? 'json-key' : 'json-string';
      } else if (/true|false/.test(match)) {
        cls = 'json-boolean';
      } else if (/null/.test(match)) {
        cls = 'json-null';
      }
      const escaped = match.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
      return `<span class="${cls}">${escaped}</span>`;
    }
  );
}

export function formatTime(timestamp) {
  if (!timestamp) return '—';
  try {
    const d = Array.isArray(timestamp)
      ? new Date(timestamp[0], timestamp[1]-1, timestamp[2], timestamp[3]||0, timestamp[4]||0, timestamp[5]||0)
      : new Date(timestamp);
    return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  } catch { return '—'; }
}

export function formatTimestamp(timestamp) {
  if (!timestamp) return '—';
  try {
    const d = Array.isArray(timestamp)
      ? new Date(timestamp[0], timestamp[1]-1, timestamp[2], timestamp[3]||0, timestamp[4]||0, timestamp[5]||0)
      : new Date(timestamp);
    return d.toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' });
  } catch { return '—'; }
}

export function formatUptime(seconds) {
  if (!seconds) return '—';
  const days  = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const mins  = Math.floor((seconds % 3600) / 60);
  return `${days}d ${hours}h ${mins}m`;
}

export function safeHighlightJson(data) {
  if (!data) return '';
  let str = data;
  if (typeof data !== 'string') {
    try {
      str = JSON.stringify(data, null, 2);
    } catch {
      str = String(data);
    }
  } else {
    try {
      const parsed = JSON.parse(data);
      str = JSON.stringify(parsed, null, 2);
    } catch {
      // keeps str as is if not valid JSON
    }
  }
  return syntaxHighlight(str);
}

export function formatMarkdown(text) {
  if (!text) return '';
  let html = escapeHtml(text);

  // Headers: ### Title, ## Title, # Title
  html = html.replace(/^### (.*$)/gim, '<h3 class="ai-heading-3">$1</h3>');
  html = html.replace(/^## (.*$)/gim, '<h2 class="ai-heading-2">$1</h2>');
  html = html.replace(/^# (.*$)/gim, '<h1 class="ai-heading-1">$1</h1>');

  // Bold text: **text**
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong class="ai-bold">$1</strong>');

  // Italic text: *text* or _text_
  html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
  html = html.replace(/_(.*?)_/g, '<em>$1</em>');

  // Inline code: `code`
  html = html.replace(/`([^`]+)`/g, '<code class="ai-code">$1</code>');

  // Bullet items: - item or * item
  html = html.replace(/^\s*[-*]\s+(.*$)/gim, '<div class="ai-list-item"><span class="ai-bullet">•</span> <span>$1</span></div>');

  // Numbered list items: 1. **Title** or 1. Item
  html = html.replace(/^\s*(\d+\.)\s+(.*$)/gim, '<div class="ai-num-item"><span class="ai-num">$1</span> <span>$2</span></div>');

  // Convert newlines to <br/>
  html = html.replace(/\n/g, '<br/>');

  // Clean up extra breaks after block elements
  html = html.replace(/(<\/h[1-3]>)\s*<br\/>/gi, '$1');
  html = html.replace(/(<\/div>)\s*<br\/>/gi, '$1');

  return html;
}

