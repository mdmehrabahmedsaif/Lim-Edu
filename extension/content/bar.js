/**
 * Lim Edu - Minimalist UI Navigation Bar & Fullscreen Controller
 * Injected cleanly into allowed websites using Shadow DOM isolation.
 */

(function () {
  'use strict';

  // Prevent multiple injections
  if (document.getElementById('lim-edu-root-host')) return;

  const SITES = [
    { id: 'course', name: 'Course (English Boli)', url: 'https://englishboli.com', hostMatch: 'englishboli.com', icon: '🎓' },
    { id: 'gmail', name: 'Gmail', url: 'https://mail.google.com', hostMatch: 'mail.google.com', icon: '✉️' },
    { id: 'keep', name: 'Google Keep', url: 'https://keep.google.com', hostMatch: 'keep.google.com', icon: '📝' },
    { id: 'account', name: 'Google Account', url: 'https://accounts.google.com', hostMatch: 'accounts.google.com', icon: '👤' }
  ];

  const currentHost = window.location.hostname.toLowerCase();

  // Create Shadow DOM host element
  const host = document.createElement('div');
  host.id = 'lim-edu-root-host';
  const shadow = host.attachShadow({ mode: 'open' });

  // Load CSS
  const cssLink = document.createElement('link');
  cssLink.rel = 'stylesheet';
  cssLink.href = chrome.runtime.getURL('content/bar.css');
  shadow.appendChild(cssLink);

  // Build Navigation Bar HTML
  const nav = document.createElement('nav');
  nav.className = 'lim-navbar';

  // Left: Brand + Quick Tabs
  const left = document.createElement('div');
  left.className = 'lim-left';

  const brand = document.createElement('div');
  brand.className = 'lim-brand';
  brand.innerHTML = `
    <div class="lim-logo-icon">L</div>
    <span class="lim-brand-title">Lim Edu</span>
  `;
  left.appendChild(brand);

  const tabsContainer = document.createElement('div');
  tabsContainer.className = 'lim-tabs';

  SITES.forEach((site) => {
    const btn = document.createElement('button');
    btn.className = 'lim-tab-btn';
    if (currentHost.includes(site.hostMatch)) {
      btn.classList.add('active');
    }
    btn.innerHTML = `<span class="lim-tab-icon">${site.icon}</span><span>${site.name}</span>`;
    btn.title = `Switch to ${site.name}`;
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      if (!currentHost.includes(site.hostMatch)) {
        window.location.href = site.url;
      }
    });
    tabsContainer.appendChild(btn);
  });
  left.appendChild(tabsContainer);

  // Center: Status Badge
  const center = document.createElement('div');
  center.className = 'lim-center';
  center.innerHTML = `
    <div class="lim-status-pill">
      <span class="lim-status-dot"></span>
      <span>Protected Mode</span>
    </div>
  `;

  // Right: Controls (Back, Forward, Reload, Fullscreen, Collapse)
  const right = document.createElement('div');
  right.className = 'lim-right';

  // Back button
  const backBtn = document.createElement('button');
  backBtn.className = 'lim-control-btn';
  backBtn.title = 'Back';
  backBtn.innerHTML = `
    <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
  `;
  backBtn.addEventListener('click', () => window.history.back());
  right.appendChild(backBtn);

  // Forward button
  const fwdBtn = document.createElement('button');
  fwdBtn.className = 'lim-control-btn';
  fwdBtn.title = 'Forward';
  fwdBtn.innerHTML = `
    <svg viewBox="0 0 24 24"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>
  `;
  fwdBtn.addEventListener('click', () => window.history.forward());
  right.appendChild(fwdBtn);

  // Reload button
  const reloadBtn = document.createElement('button');
  reloadBtn.className = 'lim-control-btn';
  reloadBtn.title = 'Reload page';
  reloadBtn.innerHTML = `
    <svg viewBox="0 0 24 24"><path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
  `;
  reloadBtn.addEventListener('click', () => window.location.reload());
  right.appendChild(reloadBtn);

  // Fullscreen toggle button
  const fsBtn = document.createElement('button');
  fsBtn.className = 'lim-control-btn';
  fsBtn.title = 'Toggle Fullscreen (Landscape)';
  fsBtn.innerHTML = `
    <svg viewBox="0 0 24 24"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/></svg>
  `;
  fsBtn.addEventListener('click', () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(err => {
        console.warn('[Lim Edu] Fullscreen request error:', err);
      });
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen().catch(() => {});
      }
    }
  });
  right.appendChild(fsBtn);

  // Collapse / Hide navbar button
  const collapseBtn = document.createElement('button');
  collapseBtn.className = 'lim-control-btn';
  collapseBtn.title = 'Collapse Navigation Bar';
  collapseBtn.innerHTML = `
    <svg viewBox="0 0 24 24"><path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/></svg>
  `;
  right.appendChild(collapseBtn);

  nav.appendChild(left);
  nav.appendChild(center);
  nav.appendChild(right);
  shadow.appendChild(nav);

  // Floating trigger to expand when collapsed
  const expandTrigger = document.createElement('button');
  expandTrigger.className = 'lim-expand-trigger';
  expandTrigger.innerHTML = `<span>Lim Edu</span> <svg style="width:10px;height:10px;fill:currentColor" viewBox="0 0 24 24"><path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/></svg>`;
  expandTrigger.title = 'Show Lim Edu Navigation Bar';
  shadow.appendChild(expandTrigger);

  // Toggle Collapse handlers
  collapseBtn.addEventListener('click', () => {
    nav.classList.add('collapsed');
    expandTrigger.classList.add('visible');
    sessionStorage.setItem('lim_edu_nav_collapsed', 'true');
  });

  expandTrigger.addEventListener('click', () => {
    nav.classList.remove('collapsed');
    expandTrigger.classList.remove('visible');
    sessionStorage.removeItem('lim_edu_nav_collapsed');
  });

  if (sessionStorage.getItem('lim_edu_nav_collapsed') === 'true') {
    nav.classList.add('collapsed');
    expandTrigger.classList.add('visible');
  }

  // Auto-hide completely during Fullscreen Mode (e.g. video fullscreen)
  function handleFullscreenChange() {
    if (document.fullscreenElement) {
      host.classList.add('fullscreen-hidden');
    } else {
      host.classList.remove('fullscreen-hidden');
    }
  }

  document.addEventListener('fullscreenchange', handleFullscreenChange);
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange);

  // Keyboard shortcut F11 for Fullscreen
  window.addEventListener('keydown', (e) => {
    if (e.key === 'F11') {
      e.preventDefault();
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen().catch(() => {});
      } else {
        document.exitFullscreen().catch(() => {});
      }
    }
  });

  // Inject host element into document
  function inject() {
    if (!document.body) {
      window.requestAnimationFrame(inject);
      return;
    }
    document.body.appendChild(host);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inject);
  } else {
    inject();
  }
})();
