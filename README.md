# Lim Edu 🎓

**Lim Edu** is a secure, ultra-lightweight, and restricted educational desktop workspace designed specifically for distraction-free learning.

---

## 🔒 Security & Domain Whitelist Enforcement

Lim Edu operates in a **Strict Whitelist Mode**. Only approved domains and essential authentication services are permitted. All outside websites, popups, and unauthorized redirects are blocked immediately and redirected to the **Lim Edu Security Shield** screen.

### Allowed Primary Workspaces:
1. **English Boli (Course):** `https://englishboli.com`
2. **Gmail:** `https://mail.google.com`
3. **Google Keep:** `https://keep.google.com`
4. **Google Account:** `https://accounts.google.com`

---

## 🎬 YouTube Embed Containment & Fullscreen Support

In English Boli (or integrated courses), video lessons frequently use YouTube embeds. Lim Edu strictly isolates these embeds:
- **No Escaping to YouTube:** The YouTube logo watermark, "Watch on YouTube" button, share button, and clickable title links to `youtube.com/watch` are disabled and hidden. Users cannot browse YouTube or view recommended outside videos.
- **Context Menu Lockdown:** Right-clicking inside video players is disabled to prevent copying outside URLs.
- **Fullscreen Experience:**
  - Clicking the player's fullscreen button immediately expands the video to 100% full screen in landscape format.
  - The navigation bar automatically auto-hides so the video occupies the full display.
  - Clicking the fullscreen exit button (or pressing `Escape` / `F11`) cleanly returns the window to normal.

---

## 🚀 How to Launch Lim Edu

You have several convenient options:

1. **Desktop Shortcut:**
   - Double-click the **Lim Edu** icon created on your Desktop.
2. **Silent Launcher (`Lim-Edu.vbs`):**
   - Double-click `Lim-Edu.vbs` to launch seamlessly without any command prompt window.
3. **Batch Launcher (`Lim-Edu.bat`):**
   - Double-click `Lim-Edu.bat`.
4. **Developer / Terminal:**
   ```bash
   npm start
   ```

---

## 🖥️ User Interface Overview

- **Minimalist Top Bar:** Injected with Shadow DOM isolation so it never conflicts with website styles.
  - **Brand & Tabs:** Switch between Course (English Boli), Gmail, Google Keep, and Google Account with a single click.
  - **Controls:** Back, Forward, Reload, and Fullscreen toggle.
  - **Collapse / Expand:** Click the collapse button anytime to tuck the bar away into a compact floating pill at top-right for complete workspace focus.
  - **Session Isolation:** All cookies, Google logins, and English Boli sessions are safely stored in `data/profile/`.

---

## ⚡ Lightweight & Compliant

- **Zero Bloat:** Uses the native, trusted Chromium runtime already present on your Windows machine.
- **Fast Startup:** Launches in under a second with minimal memory consumption.
- **Enterprise / Device Guard Compliant:** Runs smoothly without triggering Windows Device Guard executable blocks.
