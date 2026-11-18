/**
 * Lim Edu Security Shield - Background Service Worker
 * Enforces strict domain whitelisting and contains YouTube embeds.
 */

// Primary allowed hostnames for top-level browsing
const ALLOWED_PRIMARY_HOSTS = [
  'englishboli.com',
  'www.englishboli.com',
  'accounts.google.com',
  'myaccount.google.com',
  'mail.google.com',
  'keep.google.com'
];

// Essential infrastructure & authentication hosts (required for Google Login and Keep/Gmail syncing)
const ALLOWED_AUTH_AND_ASSET_HOSTS = [
  'ssl.gstatic.com',
  'fonts.googleapis.com',
  'fonts.gstatic.com',
  'apis.google.com',
  'ogs.google.com',
  'www.gstatic.com',
  'lh3.googleusercontent.com',
  'play.google.com',
  'clients6.google.com'
];

// YouTube embed resources allowed ONLY for embedded player playback
const ALLOWED_YOUTUBE_EMBED_HOSTS = [
  'www.youtube.com',
  'youtube.com',
  'www.youtube-nocookie.com',
  'googlevideo.com',
  'ytimg.com'
];

/**
 * Validates whether a given URL is permitted.
 * @param {string} rawUrl 
 * @param {boolean} isMainFrame 
 * @returns {{ allowed: boolean, reason?: string }}
 */
function checkUrlAllowed(rawUrl, isMainFrame) {
  if (!rawUrl) return { allowed: true };

  // Always permit extension internal pages (e.g. blocked.html) and blank pages
  if (rawUrl.startsWith('chrome-extension://') || rawUrl.startsWith('about:blank') || rawUrl.startsWith('chrome://')) {
    return { allowed: true };
  }

  let urlObj;
  try {
    urlObj = new URL(rawUrl);
  } catch (e) {
    return { allowed: false, reason: 'Invalid URL format' };
  }

  const hostname = urlObj.hostname.toLowerCase();
  const pathname = urlObj.pathname.toLowerCase();

  // STRICT RULE: YouTube website, watch pages, channels, feeds are ALWAYS FORBIDDEN!
  if (
    hostname.includes('youtube.com') ||
    hostname.includes('youtu.be')
  ) {
    // Only allow embed URLs inside iframes / subframes
    if (isMainFrame) {
      return { allowed: false, reason: 'YouTube website browsing is not allowed. Only course-embedded videos may play.' };
    }

    // Inside subframes, only allow embed player URLs and required streaming endpoints
    const isEmbedPlayer = pathname.startsWith('/embed/') || pathname.startsWith('/s/') || pathname.startsWith('/youtubei/') || pathname.startsWith('/api/');
    if (isEmbedPlayer) {
      return { allowed: true };
    }

    // Google accounts auth redirect uses accounts.youtube.com/accounts/SetSID
    if (hostname === 'accounts.youtube.com' && pathname.includes('/setsid')) {
      return { allowed: true };
    }

    return { allowed: false, reason: 'YouTube watch pages and external links are blocked.' };
  }

  // Check top-level (main frame) navigations
  if (isMainFrame) {
    const isPrimaryAllowed = ALLOWED_PRIMARY_HOSTS.some(host => 
      hostname === host || hostname.endsWith('.' + host)
    );

    if (isPrimaryAllowed) {
      return { allowed: true };
    }

    return { allowed: false, reason: 'This website is not on the Lim Edu allowed list.' };
  }

  // Check subframes / resources
  const isHostAllowed = 
    ALLOWED_PRIMARY_HOSTS.some(h => hostname === h || hostname.endsWith('.' + h)) ||
    ALLOWED_AUTH_AND_ASSET_HOSTS.some(h => hostname === h || hostname.endsWith('.' + h)) ||
    ALLOWED_YOUTUBE_EMBED_HOSTS.some(h => hostname === h || hostname.endsWith('.' + h));

  if (isHostAllowed) {
    return { allowed: true };
  }

  return { allowed: false, reason: 'External sub-resource blocked.' };
}

// 1. Intercept navigations before they start
chrome.webNavigation.onBeforeNavigate.addListener((details) => {
  const { url, tabId, frameId } = details;
  const isMainFrame = (frameId === 0);

  const check = checkUrlAllowed(url, isMainFrame);

  if (!check.allowed) {
    console.warn(`[Lim Edu Shield] Blocked navigation to: ${url} (MainFrame: ${isMainFrame}, Reason: ${check.reason})`);

    if (isMainFrame) {
      const blockedPage = chrome.runtime.getURL(
        `blocked.html?url=${encodeURIComponent(url)}&reason=${encodeURIComponent(check.reason || 'Restricted site')}`
      );
      chrome.tabs.update(tabId, { url: blockedPage });
    }
  }
});

// 2. Intercept new windows/tabs (e.g., target="_blank" or window.open)
chrome.webNavigation.onCreatedNavigationTarget.addListener((details) => {
  const { url, tabId } = details;
  const check = checkUrlAllowed(url, true);

  if (!check.allowed) {
    console.warn(`[Lim Edu Shield] Closing unauthorized new tab: ${url}`);
    chrome.tabs.remove(tabId);
  }
});

// 3. Listen for internal extension messages (e.g. from UI bar or blocked page)
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'navigate') {
    if (sender.tab && sender.tab.id) {
      chrome.tabs.update(sender.tab.id, { url: request.url });
    }
    sendResponse({ status: 'ok' });
  } else if (request.action === 'get_current_status') {
    sendResponse({ active: true, app: 'Lim Edu' });
  }
  return true;
});

console.log('[Lim Edu Security Shield] Active & Protecting.');
