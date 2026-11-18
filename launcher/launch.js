/**
 * Lim Edu - Native Application Launcher
 * Detects installed browser runtime (Chrome / Edge), enforces security flags,
 * mounts the isolation profile and security shield extension.
 */

const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const PROJECT_DIR = path.resolve(__dirname, '..');
const EXTENSION_DIR = path.join(PROJECT_DIR, 'extension');
const PROFILE_DIR = path.join(PROJECT_DIR, 'data', 'profile');

// Ensure data/profile directory exists
if (!fs.existsSync(PROFILE_DIR)) {
  fs.mkdirSync(PROFILE_DIR, { recursive: true });
}

// Possible browser executable paths on Windows
const BROWSER_CANDIDATES = [
  // Google Chrome
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  path.join(process.env.LOCALAPPDATA || '', 'Google\\Chrome\\Application\\chrome.exe'),
  // Microsoft Edge
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  path.join(process.env.LOCALAPPDATA || '', 'Microsoft\\Edge\\Application\\msedge.exe')
];

function findBrowser() {
  for (const candidate of BROWSER_CANDIDATES) {
    if (candidate && fs.existsSync(candidate)) {
      return candidate;
    }
  }
  return null;
}

const browserPath = findBrowser();

if (!browserPath) {
  console.error('[Lim Edu Error] No compatible browser runtime (Google Chrome or Microsoft Edge) found.');
  process.exit(1);
}

console.log(`[Lim Edu] Starting using runtime: ${browserPath}`);
console.log(`[Lim Edu] Isolated Profile: ${PROFILE_DIR}`);
console.log(`[Lim Edu] Security Shield Extension: ${EXTENSION_DIR}`);

// Target startup URL: English Boli (Course)
const START_URL = 'https://englishboli.com';

const args = [
  // App mode: standalone window without URL bar or extraneous browser buttons
  `--app=${START_URL}`,
  // User data directory for isolated logins & cookies
  `--user-data-dir=${PROFILE_DIR}`,
  // Strict extension loading (only Lim Edu Security Shield enabled)
  `--load-extension=${EXTENSION_DIR}`,
  `--disable-extensions-except=${EXTENSION_DIR}`,
  // Window geometry
  '--window-size=1280,820',
  '--start-maximized',
  // Performance and isolation optimizations
  '--no-first-run',
  '--no-default-browser-check',
  '--disable-sync',
  '--disable-background-networking',
  '--disable-default-apps',
  '--disable-component-update',
  '--disable-features=Translate,OptimizationHints,MediaRouter'
];

const child = spawn(browserPath, args, {
  detached: true,
  stdio: 'ignore'
});

child.unref();

console.log('[Lim Edu] Application launched successfully. Enjoy learning!');
process.exit(0);
