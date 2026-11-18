/**
 * Lim Edu - YouTube Embed Controller
 * Prevents navigation out to YouTube website while maintaining full video controls & fullscreen.
 */

(function() {
  'use strict';

  // Intercept all link clicks inside the YouTube iframe
  document.addEventListener('click', (e) => {
    const anchor = e.target.closest('a');
    if (anchor) {
      const href = anchor.href || '';
      // Block any navigation to youtube.com/watch, youtu.be, or channels
      if (
        href.includes('youtube.com/watch') ||
        href.includes('youtu.be') ||
        href.includes('youtube.com/channel') ||
        href.includes('youtube.com/user') ||
        href.includes('youtube.com/c/')
      ) {
        e.preventDefault();
        e.stopPropagation();
        e.stopImmediatePropagation();
        console.log('[Lim Edu] Blocked click escaping YouTube embed:', href);
        return false;
      }
    }
  }, true);

  // Disable context menu to prevent "Copy video URL" or "Watch on YouTube"
  document.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    e.stopPropagation();
    return false;
  }, true);

  // Override window.open inside the embed iframe to prevent popups
  try {
    const script = document.createElement('script');
    script.textContent = `
      (function() {
        window.open = function(url) {
          console.warn('[Lim Edu] Blocked window.open from YouTube player:', url);
          return null;
        };
      })();
    `;
    (document.head || document.documentElement).appendChild(script);
    script.remove();
  } catch (err) {
    // Ignore context script isolation errors
  }

  // Ensure fullscreen keyboard shortcut works cleanly
  document.addEventListener('keydown', (e) => {
    if (e.key === 'f' || e.key === 'F') {
      const video = document.querySelector('video') || document.querySelector('.html5-main-video');
      if (video) {
        if (!document.fullscreenElement) {
          if (video.requestFullscreen) {
            video.requestFullscreen().catch(() => {});
          }
        } else {
          if (document.exitFullscreen) {
            document.exitFullscreen().catch(() => {});
          }
        }
      }
    }
  });

  console.log('[Lim Edu] YouTube embed protection initialized.');
})();
