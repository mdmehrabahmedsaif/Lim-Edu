package com.limedu.app;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LimEdu";

    private static final String PREF_NAME = "lim_edu_prefs";
    private static final String PREF_KEY_COURSE_URL = "last_url_course";
    private static final String PREF_KEY_ONONNO_URL = "last_url_ononno";
    private static final String PREF_KEY_GMAIL_URL = "last_url_gmail";
    private static final String PREF_KEY_KEEP_URL = "last_url_keep";
    private static final String PREF_KEY_ACCOUNT_URL = "last_url_account";
    private static final String PREF_KEY_LAST_TAB = "last_active_tab";
    private static final String PREF_VIDEO_TIMES = "lim_edu_video_times";

    private static final String URL_COURSE = "https://englishboli.com";
    private static final String URL_ONONNO = "https://www.ononno.com";
    private static final String URL_GMAIL = "https://mail.google.com";
    private static final String URL_KEEP = "https://keep.google.com";
    private static final String URL_ACCOUNT = "https://accounts.google.com";

    // Dedicated independent WebViews for seamless tab switching without reloading
    private WebView webViewCourse;
    private WebView webViewOnonno;
    private WebView webViewGmail;
    private WebView webViewKeep;
    private WebView webViewAccount;

    private ProgressBar progressBar;
    private FrameLayout fullscreenContainer;
    private View customVideoView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private LinearLayout topBar;
    private LinearLayout bottomBar;
    private FrameLayout webviewContainer;
    private ScrollView homeContainer;
    private ScrollView settingsContainer;
    private TextView appTitle;

    private ImageView iconHome;
    private ImageView iconCourse;
    private ImageView iconGmail;
    private ImageView iconKeep;
    private ImageView iconSettings;

    private TextView labelHome;
    private TextView labelCourse;
    private TextView labelGmail;
    private TextView labelKeep;
    private TextView labelSettings;

    private LinearLayout tabHomePill;
    private LinearLayout tabCoursePill;
    private LinearLayout tabGmailPill;
    private LinearLayout tabKeepPill;
    private LinearLayout tabSettingsPill;

    private TextView labelFullscreenStatus;

    private boolean isManualFullscreen = false;
    private String cachedUserAgent = "";
    private String currentTab = "home";

    // Consecutive click tracking for 3+ rapid clicks to reset tab to site default
    private String lastClickedTab = "";
    private int tabClickCount = 0;
    private long lastTabClickTime = 0;

    private static final String YOUTUBE_CLEAN_SELECTORS = 
        "'[class*=\"RelatedVideosEntryPoint\"]', '[class*=\"ytFullscreenVideoRecommendations\"]', " +
        "'ytm-endscreen-video-renderer', 'ytm-video-with-context-renderer', " +
        "'.ytwPlayerBottomControlsWatchOnYouTubeButton', " +
        "'.iv-branding', '.iv-branding-active', '.branding-img-container', '.branding-img', '.branding-context-container', " +
        "'.annotation', '.annotation-type-custom', '.iv-card', '.iv-drawer', " +
        "'.ytp-cards-teaser', '.ytp-cards-teaser-box', '.ytp-cards-teaser-text', '.ytp-cards-button', " +
        "'.ytp-ce-element', '.ytp-ce-video', '.ytp-ce-channel', '.ytp-ce-playlist', " +
        "'.ytp-suggested-action', '.ytp-suggested-action-badge', '.ytp-featured-product', '.ytp-featured-video', " +
        "'.ytp-youtube-button', '.ytp-watermark', '.ytp-watermark-small', '.ytp-share-button', '.ytp-copylink-button', " +
        "'.ytp-pause-overlay', '.ytp-pause-overlay-container', 'a.ytp-title-channel-logo', '.ytp-promotooltip-wrapper'";

    private static final String YOUTUBE_CLEAN_CSS = 
        /* Mobile Polymer Suggestion entry card and recommendations drawer only */
        "[class*='RelatedVideosEntryPoint'], [class*='ytFullscreenVideoRecommendations'], " +
        "ytm-endscreen-video-renderer, ytm-video-with-context-renderer, " +
        ".ytwPlayerBottomControlsWatchOnYouTubeButton, " +
        /* Desktop & standard player teasers, branding watermarks & outside links */
        ".ytp-cards-teaser, .ytp-cards-teaser-box, .ytp-cards-teaser-text, .ytp-cards-button, " +
        ".iv-branding, .iv-branding-active, .branding-img-container, .branding-img, .branding-context-container, " +
        ".annotation, .annotation-type-custom, .iv-card, .iv-drawer, " +
        ".ytp-ce-element, .ytp-ce-element-show, .ytp-ce-video, .ytp-ce-channel, .ytp-ce-playlist, " +
        ".ytp-suggestions, .ytp-suggestions-container, .ytp-suggestion-panel, " +
        ".ytp-suggested-action, .ytp-suggested-action-badge, .ytp-featured-video, .ytp-featured-product, " +
        ".ytp-pause-overlay, .ytp-pause-overlay-container, " +
        ".ytp-youtube-button, .ytp-watermark, .ytp-watermark-small, .ytp-share-button, .ytp-copylink-button, " +
        "a.ytp-title-channel-logo, .ytp-cued-thumbnail-overlay-image { " +
        "display: none !important; visibility: hidden !important; opacity: 0 !important; " +
        "pointer-events: none !important; width: 0px !important; height: 0px !important; margin: 0px !important; padding: 0px !important; } " +
        /* Disable external links */
        ".ytp-title-link, a[href*='youtube.com'], a[href*='youtu.be'] { " +
        "pointer-events: none !important; cursor: default !important; } " +
        /* Ensure player controls overlay and all tools are fully functional when tapped */
        "#player-control-overlay, .player-controls-content, .player-controls-middle, " +
        ".player-controls-top, .player-controls-bottom, .ytp-chrome-bottom, .ytp-chrome-controls, " +
        ".ytp-play-button, .ytp-volume-panel, .ytp-time-display, " +
        ".ytp-fullscreen-button { pointer-events: auto !important; } ";

    /**
     * Ultra-lightweight JavaScript Bridge for persisting video timestamps across app restarts.
     * Uses zero polling, purely event-driven, minimal RAM & battery footprint.
     */
    public class LimEduVideoBridge {
        @JavascriptInterface
        public void savePageUrl(String tab, String url) {
            if (tab == null || url == null || url.trim().isEmpty()) return;
            runOnUiThread(() -> saveLastUrl(tab, url));
        }

        @JavascriptInterface
        public void saveVideoTime(String videoId, float seconds) {
            if (videoId == null || videoId.isEmpty()) return;
            try {
                SharedPreferences prefs = getSharedPreferences(PREF_VIDEO_TIMES, MODE_PRIVATE);
                String key = videoId.startsWith("v_") ? videoId : ("v_" + videoId);
                if (seconds <= 2) {
                    prefs.edit().remove(key).remove(videoId).apply();
                } else {
                    prefs.edit().putFloat(key, seconds).putFloat(videoId, seconds).apply();
                }
                Log.d(TAG, "Video timestamp persisted: [" + key + "] -> " + seconds + "s");
            } catch (Exception e) {
                Log.w(TAG, "Error saving video timestamp", e);
            }
        }

        @JavascriptInterface
        public float getVideoTime(String videoId) {
            if (videoId == null || videoId.isEmpty()) return 0f;
            try {
                SharedPreferences prefs = getSharedPreferences(PREF_VIDEO_TIMES, MODE_PRIVATE);
                float time = prefs.getFloat("v_" + videoId, 0f);
                if (time <= 0) {
                    time = prefs.getFloat(videoId, 0f);
                }
                if (time > 0) {
                    Log.d(TAG, "Retrieved video timestamp for [" + videoId + "]: " + time + "s");
                }
                return time;
            } catch (Exception e) {
                return 0f;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWebViews();
        setupNavigationTabs();
        setupTopControls();
        setupBackNavigation();

        // Restore last active study space across app restarts / background kills
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String lastTab = prefs.getString(PREF_KEY_LAST_TAB, "home");
        if ("ononno".equals(lastTab)) {
            switchToWebTab("ononno");
        } else if ("course".equals(lastTab)) {
            switchToWebTab("course");
        } else if ("gmail".equals(lastTab)) {
            switchToWebTab("gmail");
        } else if ("keep".equals(lastTab)) {
            switchToWebTab("keep");
        } else if ("settings".equals(lastTab)) {
            showSettingsScreen();
        } else {
            showHomeScreen();
        }
    }

    private void initViews() {
        webViewCourse = findViewById(R.id.webview_course);
        webViewOnonno = findViewById(R.id.webview_ononno);
        webViewGmail = findViewById(R.id.webview_gmail);
        webViewKeep = findViewById(R.id.webview_keep);
        webViewAccount = findViewById(R.id.webview_account);

        webViewCourse.setTag("course");
        webViewOnonno.setTag("ononno");
        webViewGmail.setTag("gmail");
        webViewKeep.setTag("keep");
        webViewAccount.setTag("account");

        progressBar = findViewById(R.id.progress_bar);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        topBar = findViewById(R.id.top_bar);
        bottomBar = findViewById(R.id.bottom_bar);
        webviewContainer = findViewById(R.id.webview_container);
        homeContainer = findViewById(R.id.home_container);
        settingsContainer = findViewById(R.id.settings_container);
        appTitle = findViewById(R.id.app_title);

        iconHome = findViewById(R.id.icon_home);
        iconCourse = findViewById(R.id.icon_course);
        iconGmail = findViewById(R.id.icon_gmail);
        iconKeep = findViewById(R.id.icon_keep);
        iconSettings = findViewById(R.id.icon_settings);

        labelHome = findViewById(R.id.label_home);
        labelCourse = findViewById(R.id.label_course);
        labelGmail = findViewById(R.id.label_gmail);
        labelKeep = findViewById(R.id.label_keep);
        labelSettings = findViewById(R.id.label_settings);

        tabHomePill = findViewById(R.id.tab_home_pill);
        tabCoursePill = findViewById(R.id.tab_course_pill);
        tabGmailPill = findViewById(R.id.tab_gmail_pill);
        tabKeepPill = findViewById(R.id.tab_keep_pill);
        tabSettingsPill = findViewById(R.id.tab_settings_pill);

        labelFullscreenStatus = findViewById(R.id.label_fullscreen_status);
    }

    private void setupWebViews() {
        WebView.setWebContentsDebuggingEnabled(true);
        setupSingleWebView(webViewCourse);
        setupSingleWebView(webViewOnonno);
        setupSingleWebView(webViewGmail);
        setupSingleWebView(webViewKeep);
        setupSingleWebView(webViewAccount);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupSingleWebView(WebView wv) {
        if (wv == null) return;
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Responsive mobile viewport & smooth hardware-accelerated touch interaction
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        wv.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        wv.setVerticalScrollBarEnabled(true);
        wv.setHorizontalScrollBarEnabled(false);
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Security lockdowns
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        // Clean modern UA for Google Auth
        if (cachedUserAgent.isEmpty()) {
            String originalUA = settings.getUserAgentString();
            cachedUserAgent = originalUA.replace("; wv", "");
        }
        settings.setUserAgentString(cachedUserAgent);

        // Cookies setup
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(wv, true);

        // Native PDF & File Download Listener
        wv.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                downloadFile(url, userAgent, contentDisposition, mimetype);
            }
        });

        // Add lightweight bridge for video timestamps
        wv.addJavascriptInterface(new LimEduVideoBridge(), "LimEduBridge");

        wv.setWebViewClient(new LimEduWebViewClient());
        wv.setWebChromeClient(new LimEduWebChromeClient());
    }

    /**
     * Downloads files (e.g. course exercise PDFs) directly to the phone's Download directory.
     */
    private void downloadFile(String url, String userAgent, String contentDisposition, String mimetype) {
        try {
            if (url == null || url.isEmpty()) return;

            String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
            if ((mimetype != null && mimetype.contains("pdf")) && !filename.toLowerCase().endsWith(".pdf")) {
                filename += ".pdf";
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            if (mimetype != null && !mimetype.isEmpty()) {
                request.setMimeType(mimetype);
            }

            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) {
                request.addRequestHeader("cookie", cookies);
            }
            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            request.setDescription("Downloading file from Lim Edu...");
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Downloading PDF: " + filename, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Download error", e);
            Toast.makeText(this, "Failed to start download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNavigationTabs() {
        // Bottom bar tab clicks with triple-click detection
        findViewById(R.id.tab_home).setOnClickListener(v -> onTabClicked("home"));
        findViewById(R.id.tab_course).setOnClickListener(v -> onTabClicked("course"));
        findViewById(R.id.tab_gmail).setOnClickListener(v -> onTabClicked("gmail"));
        findViewById(R.id.tab_keep).setOnClickListener(v -> onTabClicked("keep"));
        findViewById(R.id.tab_settings).setOnClickListener(v -> onTabClicked("settings"));

        // Home screen interactive destination cards
        findViewById(R.id.card_course).setOnClickListener(v -> switchToWebTab("course"));
        View cardOnonno = findViewById(R.id.card_ononno);
        if (cardOnonno != null) cardOnonno.setOnClickListener(v -> switchToWebTab("ononno"));
        findViewById(R.id.card_gmail).setOnClickListener(v -> switchToWebTab("gmail"));
        findViewById(R.id.card_keep).setOnClickListener(v -> switchToWebTab("keep"));
        findViewById(R.id.card_downloads).setOnClickListener(v -> openDownloadsFolder());

        // Dedicated Settings Page controls
        findViewById(R.id.setting_fullscreen).setOnClickListener(v -> toggleAppFullscreen());
        findViewById(R.id.setting_reload).setOnClickListener(v -> {
            WebView active = getActiveWebView();
            if (active != null && active.getUrl() != null && !active.getUrl().equals("about:blank")) {
                active.reload();
                Toast.makeText(this, "Reloading current page...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No active web page to reload.", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.setting_account).setOnClickListener(v -> switchToWebTab("account"));
        findViewById(R.id.setting_logout).setOnClickListener(v -> promptSignOut());
        findViewById(R.id.setting_clear_cache).setOnClickListener(v -> promptClearCache());
        findViewById(R.id.setting_downloads).setOnClickListener(v -> openDownloadsFolder());
    }

    /**
     * Handles tab button clicks, including 3+ rapid consecutive clicks to reset to default root page.
     */
    private void onTabClicked(String tab) {
        long now = System.currentTimeMillis();
        if (tab.equals(lastClickedTab) && (now - lastTabClickTime < 1500)) {
            tabClickCount++;
        } else {
            lastClickedTab = tab;
            tabClickCount = 1;
        }
        lastTabClickTime = now;

        // Reset to default page on 3 or more rapid clicks
        if (tabClickCount >= 3) {
            tabClickCount = 0;
            resetTabToDefault(tab);
            return;
        }

        if ("home".equals(tab)) {
            showHomeScreen();
        } else if ("settings".equals(tab)) {
            showSettingsScreen();
        } else if ("course".equals(tab) || "ononno".equals(tab)) {
            showCourseSelectorSheet();
        } else {
            // If already on this tab and web content is visible, stay where we are without reloading
            if (tab.equals(currentTab) && webviewContainer != null && webviewContainer.getVisibility() == View.VISIBLE) {
                return;
            }
            switchToWebTab(tab);
        }
    }

    /**
     * Shows the Course Selector Bottom Sheet when the bottom Course tab is tapped,
     * allowing the user to seamlessly jump between EnglishBoli and Ononno.
     */
    private void showCourseSelectorSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_course_selector, null);
        dialog.setContentView(sheetView);

        if (dialog.getWindow() != null) {
            dialog.setOnShowListener(d -> {
                FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    bottomSheet.setBackgroundResource(android.R.color.transparent);
                }
            });
        }

        sheetView.findViewById(R.id.item_course_englishboli).setOnClickListener(v -> {
            dialog.dismiss();
            switchToWebTab("course");
        });
        sheetView.findViewById(R.id.item_course_ononno).setOnClickListener(v -> {
            dialog.dismiss();
            switchToWebTab("ononno");
        });

        View btnClose = sheetView.findViewById(R.id.btn_close_sheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    /**
     * Resets a tab's WebView and persisted storage to its absolute default home page.
     */
    private void resetTabToDefault(String tab) {
        WebView targetView = null;
        String defaultUrl = null;
        String prefKey = null;
        String tabName = "";

        switch (tab) {
            case "course":
                targetView = webViewCourse;
                defaultUrl = URL_COURSE;
                prefKey = PREF_KEY_COURSE_URL;
                tabName = "EnglishBoli";
                break;
            case "ononno":
                targetView = webViewOnonno;
                defaultUrl = URL_ONONNO;
                prefKey = PREF_KEY_ONONNO_URL;
                tabName = "Ononno";
                break;
            case "gmail":
                targetView = webViewGmail;
                defaultUrl = URL_GMAIL;
                prefKey = PREF_KEY_GMAIL_URL;
                tabName = "Gmail";
                break;
            case "keep":
                targetView = webViewKeep;
                defaultUrl = URL_KEEP;
                prefKey = PREF_KEY_KEEP_URL;
                tabName = "Google Keep";
                break;
            case "account":
                targetView = webViewAccount;
                defaultUrl = URL_ACCOUNT;
                prefKey = PREF_KEY_ACCOUNT_URL;
                tabName = "Google Account";
                break;
            case "home":
                showHomeScreen();
                return;
            case "settings":
                showSettingsScreen();
                return;
        }

        if (targetView != null && defaultUrl != null) {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            prefs.edit().putString(prefKey, defaultUrl).apply();

            switchToWebTab(tab);
            targetView.loadUrl(defaultUrl);

            Toast.makeText(this, tabName + " ডিফল্ট পেইজে নেওয়া হয়েছে", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Triple-click: Reset tab [" + tab + "] to default URL: " + defaultUrl);
        }
    }

    private void openDownloadsFolder() {
        try {
            Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Downloads: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    public void showHomeScreen() {
        currentTab = "home";
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putString(PREF_KEY_LAST_TAB, "home").apply();
        if (homeContainer != null) homeContainer.setVisibility(View.VISIBLE);
        if (settingsContainer != null) settingsContainer.setVisibility(View.GONE);
        if (webviewContainer != null) webviewContainer.setVisibility(View.GONE);
        if (topBar != null) topBar.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        updateTabHighlights("home");

        pauseAllWebViews();
    }

    public void showSettingsScreen() {
        currentTab = "settings";
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putString(PREF_KEY_LAST_TAB, "settings").apply();
        if (homeContainer != null) homeContainer.setVisibility(View.GONE);
        if (settingsContainer != null) settingsContainer.setVisibility(View.VISIBLE);
        if (webviewContainer != null) webviewContainer.setVisibility(View.GONE);
        if (topBar != null) topBar.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        updateTabHighlights("settings");

        pauseAllWebViews();
    }

    private void pauseAllWebViews() {
        if (webViewCourse != null) webViewCourse.onPause();
        if (webViewOnonno != null) webViewOnonno.onPause();
        if (webViewGmail != null) webViewGmail.onPause();
        if (webViewKeep != null) webViewKeep.onPause();
        if (webViewAccount != null) webViewAccount.onPause();
    }

    public void switchToWebTab(String tab) {
        currentTab = tab;
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putString(PREF_KEY_LAST_TAB, tab).apply();

        if (homeContainer != null) homeContainer.setVisibility(View.GONE);
        if (settingsContainer != null) settingsContainer.setVisibility(View.GONE);
        if (webviewContainer != null) webviewContainer.setVisibility(View.VISIBLE);
        if (topBar != null && !isManualFullscreen) topBar.setVisibility(View.VISIBLE);

        updateTabHighlights(tab);

        WebView targetView = null;
        String targetTitle = "Lim Edu";
        String prefKey = null;
        String defaultUrl = null;

        switch (tab) {
            case "course":
                targetView = webViewCourse;
                targetTitle = "EnglishBoli";
                prefKey = PREF_KEY_COURSE_URL;
                defaultUrl = URL_COURSE;
                break;
            case "ononno":
                targetView = webViewOnonno;
                targetTitle = "Ononno";
                prefKey = PREF_KEY_ONONNO_URL;
                defaultUrl = URL_ONONNO;
                break;
            case "gmail":
                targetView = webViewGmail;
                targetTitle = "Gmail";
                prefKey = PREF_KEY_GMAIL_URL;
                defaultUrl = URL_GMAIL;
                break;
            case "keep":
                targetView = webViewKeep;
                targetTitle = "Google Keep";
                prefKey = PREF_KEY_KEEP_URL;
                defaultUrl = URL_KEEP;
                break;
            case "account":
                targetView = webViewAccount;
                targetTitle = "Google Account";
                prefKey = PREF_KEY_ACCOUNT_URL;
                defaultUrl = URL_ACCOUNT;
                break;
        }

        if (appTitle != null) {
            appTitle.setText(targetTitle);
        }

        // Hide and pause inactive WebViews to save battery and mute audio
        WebView[] allViews = new WebView[]{webViewCourse, webViewOnonno, webViewGmail, webViewKeep, webViewAccount};
        for (WebView wv : allViews) {
            if (wv != null && wv != targetView) {
                wv.setVisibility(View.GONE);
                wv.onPause();
            }
        }

        if (targetView != null) {
            targetView.setVisibility(View.VISIBLE);
            targetView.onResume();

            // If this tab's WebView has not loaded anything yet (fresh app start), load saved URL or default
            String currentUrl = targetView.getUrl();
            if (currentUrl == null || currentUrl.isEmpty() || "about:blank".equals(currentUrl)) {
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                String savedUrl = prefs.getString(prefKey, defaultUrl);
                if (savedUrl == null || savedUrl.isEmpty() || !isValidSaveUrl(tab, savedUrl)) {
                    savedUrl = defaultUrl;
                }
                Log.d(TAG, "Restoring/loading URL for tab [" + tab + "]: " + savedUrl);
                targetView.loadUrl(savedUrl);
            } else {
                Log.d(TAG, "Tab [" + tab + "] resumed at current state: " + currentUrl);
            }
        }
    }

    private WebView getActiveWebView() {
        if (webviewContainer != null && webviewContainer.getVisibility() == View.VISIBLE) {
            switch (currentTab) {
                case "course":
                    return webViewCourse;
                case "ononno":
                    return webViewOnonno;
                case "gmail":
                    return webViewGmail;
                case "keep":
                    return webViewKeep;
                case "account":
                    return webViewAccount;
            }
        }
        return null;
    }

    private void setupTopControls() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackAction());
        }

        ImageButton btnForward = findViewById(R.id.btn_forward);
        if (btnForward != null) {
            btnForward.setOnClickListener(v -> {
                WebView active = getActiveWebView();
                navigateWebForward(active);
            });
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customVideoView != null) {
                    exitVideoFullscreen();
                } else if (isManualFullscreen) {
                    toggleAppFullscreen();
                } else {
                    handleBackAction();
                }
            }
        });
    }

    /**
     * Unified back action handler for top bar arrow and Android system back gesture.
     */
    private void handleBackAction() {
        if (webviewContainer != null && webviewContainer.getVisibility() == View.VISIBLE) {
            WebView active = getActiveWebView();
            navigateWebBack(active);
        } else if (settingsContainer != null && settingsContainer.getVisibility() == View.VISIBLE) {
            showHomeScreen();
        } else {
            finish();
        }
    }

    /**
     * Robust web back navigation that avoids getting trapped in redirect loops,
     * navigates up within subpages if history is blank, and only exits to Home when at site root.
     */
    private void navigateWebBack(WebView active) {
        if (active == null) {
            showHomeScreen();
            return;
        }

        WebBackForwardList history = active.copyBackForwardList();
        int currentIndex = history.getCurrentIndex();

        // 1. If WebView has backward history, find the previous distinct URL (skipping bounce redirects)
        if (currentIndex > 0) {
            String currentUrl = active.getUrl();
            int targetIndex = -1;

            for (int i = currentIndex - 1; i >= 0; i--) {
                WebHistoryItem item = history.getItemAtIndex(i);
                if (item != null) {
                    String itemUrl = item.getUrl();
                    if (itemUrl != null && !isSamePageOrRedirect(currentUrl, itemUrl)) {
                        targetIndex = i;
                        break;
                    }
                }
            }

            if (targetIndex >= 0) {
                int steps = targetIndex - currentIndex;
                Log.d(TAG, "Navigating back " + steps + " step(s) to: " + history.getItemAtIndex(targetIndex).getUrl());
                active.goBackOrForward(steps);
                return;
            } else if (active.canGoBack()) {
                active.goBack();
                return;
            }
        }

        // 2. If there is no WebView back history (e.g. freshly restored URL on app startup),
        // check if user is on a subpage of the site. If so, navigate up to the site's default root page!
        String currentUrl = active.getUrl();
        String defaultUrl = getDefaultUrlForTab(currentTab);
        if (currentUrl != null && defaultUrl != null && !isRootPage(currentUrl, defaultUrl)) {
            Log.d(TAG, "No back history, navigating up to site root: " + defaultUrl);
            active.loadUrl(defaultUrl);
            return;
        }

        // 3. Only when user is already at the site's root page and clicks Back, return to the Lim Edu Home screen
        showHomeScreen();
    }

    /**
     * Navigates forward to the next distinct URL in the WebView history.
     */
    private void navigateWebForward(WebView active) {
        if (active == null) return;

        WebBackForwardList history = active.copyBackForwardList();
        int currentIndex = history.getCurrentIndex();
        int totalItems = history.getSize();

        if (currentIndex < totalItems - 1) {
            String currentUrl = active.getUrl();
            int targetIndex = -1;

            for (int i = currentIndex + 1; i < totalItems; i++) {
                WebHistoryItem item = history.getItemAtIndex(i);
                if (item != null) {
                    String itemUrl = item.getUrl();
                    if (itemUrl != null && !isSamePageOrRedirect(currentUrl, itemUrl)) {
                        targetIndex = i;
                        break;
                    }
                }
            }

            if (targetIndex > currentIndex) {
                int steps = targetIndex - currentIndex;
                Log.d(TAG, "Navigating forward " + steps + " step(s) to: " + history.getItemAtIndex(targetIndex).getUrl());
                active.goBackOrForward(steps);
                return;
            } else if (active.canGoForward()) {
                active.goForward();
            }
        }
    }

    private String getDefaultUrlForTab(String tab) {
        if ("course".equals(tab)) return URL_COURSE;
        if ("ononno".equals(tab)) return URL_ONONNO;
        if ("gmail".equals(tab)) return URL_GMAIL;
        if ("keep".equals(tab)) return URL_KEEP;
        if ("account".equals(tab)) return URL_ACCOUNT;
        return null;
    }

    private boolean isRootPage(String currentUrl, String defaultUrl) {
        if (currentUrl == null || defaultUrl == null) return true;
        try {
            Uri currentUri = Uri.parse(currentUrl);
            Uri defaultUri = Uri.parse(defaultUrl);

            String curHost = currentUri.getHost() != null ? currentUri.getHost().toLowerCase() : "";
            String defHost = defaultUri.getHost() != null ? defaultUri.getHost().toLowerCase() : "";
            if (!curHost.equals(defHost) && !curHost.endsWith("." + defHost)) return false;

            String curPath = currentUri.getPath() != null ? currentUri.getPath().replaceAll("/+$", "") : "";
            String defPath = defaultUri.getPath() != null ? defaultUri.getPath().replaceAll("/+$", "") : "";

            return curPath.equalsIgnoreCase(defPath) || curPath.isEmpty();
        } catch (Exception e) {
            return currentUrl.equalsIgnoreCase(defaultUrl);
        }
    }

    private boolean isSamePageOrRedirect(String url1, String url2) {
        if (url1 == null || url2 == null) return false;
        if (url1.equals(url2)) return true;

        String clean1 = url1.replaceAll("/+$", "").replaceAll("#.*$", "");
        String clean2 = url2.replaceAll("/+$", "").replaceAll("#.*$", "");
        if (clean1.equalsIgnoreCase(clean2)) return true;

        String noProto1 = clean1.replaceFirst("^https?://", "");
        String noProto2 = clean2.replaceFirst("^https?://", "");
        return noProto1.equalsIgnoreCase(noProto2);
    }

    private void promptSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Do you want to log out, reset all tab states, and clear sessions?")
                .setPositiveButton("Sign Out", (dialog, which) -> performSignOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performSignOut() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(value -> {
            cookieManager.flush();
            Log.d(TAG, "Cookies cleared: " + value);
        });

        WebStorage.getInstance().deleteAllData();

        WebView[] allViews = new WebView[]{webViewCourse, webViewOnonno, webViewGmail, webViewKeep, webViewAccount};
        for (WebView wv : allViews) {
            if (wv != null) {
                wv.clearCache(true);
                wv.clearFormData();
                wv.clearHistory();
                wv.loadUrl("about:blank");
            }
        }

        // Reset saved URLs and video timestamps
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();
        getSharedPreferences(PREF_VIDEO_TIMES, MODE_PRIVATE).edit().clear().apply();

        showHomeScreen();

        Toast.makeText(this, "Signed out and reset successfully.", Toast.LENGTH_SHORT).show();
    }

    private void promptClearCache() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Browser Cache")
                .setMessage("Do you want to delete temporary web cache and storage?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    WebView[] allViews = new WebView[]{webViewCourse, webViewOnonno, webViewGmail, webViewKeep, webViewAccount};
                    for (WebView wv : allViews) {
                        if (wv != null) {
                            wv.clearCache(true);
                            wv.clearFormData();
                        }
                    }
                    WebStorage.getInstance().deleteAllData();
                    Toast.makeText(this, "Cache cleared successfully.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleAppFullscreen() {
        isManualFullscreen = !isManualFullscreen;
        if (isManualFullscreen) {
            if (topBar != null) topBar.setVisibility(View.GONE);
            if (bottomBar != null) bottomBar.setVisibility(View.GONE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            hideSystemBars();
        } else {
            if (webviewContainer != null && webviewContainer.getVisibility() == View.VISIBLE && topBar != null) {
                topBar.setVisibility(View.VISIBLE);
            }
            if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            showSystemBars();
        }
        if (labelFullscreenStatus != null) {
            labelFullscreenStatus.setText(isManualFullscreen ? "Exit" : "Toggle");
        }
    }

    private void updateTabHighlights(String activeTag) {
        if (activeTag != null) {
            currentTab = activeTag;
        }
        int activeColor = ContextCompat.getColor(this, R.color.nav_active);
        int inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive);

        boolean isHome = "home".equals(currentTab);
        boolean isSettings = "settings".equals(currentTab);
        boolean isCourse = "course".equals(currentTab) || "ononno".equals(currentTab);
        boolean isGmail = "gmail".equals(currentTab);
        boolean isKeep = "keep".equals(currentTab);

        setTabState(tabHomePill, iconHome, labelHome, isHome, activeColor, inactiveColor);
        setTabState(tabCoursePill, iconCourse, labelCourse, isCourse, activeColor, inactiveColor);
        setTabState(tabGmailPill, iconGmail, labelGmail, isGmail, activeColor, inactiveColor);
        setTabState(tabKeepPill, iconKeep, labelKeep, isKeep, activeColor, inactiveColor);
        setTabState(tabSettingsPill, iconSettings, labelSettings, isSettings, activeColor, inactiveColor);
    }

    private void setTabState(LinearLayout pill, ImageView icon, TextView label, boolean isActive, int activeColor, int inactiveColor) {
        if (pill != null) {
            pill.setBackgroundResource(isActive ? R.drawable.bg_tab_pill_active : android.R.color.transparent);
        }
        if (icon != null) {
            icon.setColorFilter(isActive ? activeColor : inactiveColor);
        }
        if (label != null) {
            label.setTextColor(isActive ? activeColor : inactiveColor);
            label.setTypeface(null, isActive ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    /**
     * Checks if a URL is valid to persist for a given tab.
     */
    private boolean isValidSaveUrl(String tab, String url) {
        if (url == null || url.trim().isEmpty()) return false;
        if (url.startsWith("about:") || url.contains("chromewebdata") || url.startsWith("data:") || url.startsWith("javascript:")) {
            return false;
        }
        if ("course".equals(tab)) {
            return (url.contains("englishboli.com") && !isBlogUrl(url, Uri.parse(url).getPath()));
        } else if ("ononno".equals(tab)) {
            return url.contains("ononno.com") || url.contains("fahimabdullah.com") || url.contains("klasio.com");
        } else if ("gmail".equals(tab)) {
            return url.contains("mail.google.com") || url.contains("accounts.google.com");
        } else if ("keep".equals(tab)) {
            return url.contains("keep.google.com") || url.contains("accounts.google.com");
        } else if ("account".equals(tab)) {
            return url.contains("accounts.google.com") || url.contains("myaccount.google.com");
        }
        return false;
    }

    /**
     * Persists the last visited URL of a tab into SharedPreferences so it survives app kills.
     */
    private void saveLastUrl(String tab, String url) {
        if (tab == null || url == null) return;
        if (!isValidSaveUrl(tab, url)) return;

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        switch (tab) {
            case "course":
                editor.putString(PREF_KEY_COURSE_URL, url);
                break;
            case "ononno":
                editor.putString(PREF_KEY_ONONNO_URL, url);
                break;
            case "gmail":
                editor.putString(PREF_KEY_GMAIL_URL, url);
                break;
            case "keep":
                editor.putString(PREF_KEY_KEEP_URL, url);
                break;
            case "account":
                editor.putString(PREF_KEY_ACCOUNT_URL, url);
                break;
        }
        editor.apply();
        Log.d(TAG, "Persisted last URL for [" + tab + "]: " + url);
    }

    /**
     * Domain Security & Strict Whitelist Verification.
     */
    private boolean isAllowedUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;

        if (url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("about:") || url.startsWith("javascript:")) {
            return true;
        }

        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception e) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null) return false;
        scheme = scheme.toLowerCase();
        if (!scheme.equals("https") && !scheme.equals("http")) {
            return false;
        }

        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase();

        String path = uri.getPath() != null ? uri.getPath().toLowerCase() : "";

        // 1. Google OAuth broker
        if (host.equals("accounts.youtube.com") || host.endsWith(".accounts.youtube.com")) {
            return true;
        }

        // 2. YouTube embed player containment
        if (host.equals("youtube.com") || host.endsWith(".youtube.com") ||
            host.equals("youtube-nocookie.com") || host.endsWith(".youtube-nocookie.com") ||
            host.equals("youtu.be")) {

            boolean isEmbed = path.startsWith("/embed/") ||
                              path.startsWith("/s/") ||
                              path.startsWith("/youtubei/") ||
                              path.startsWith("/api/") ||
                              path.startsWith("/generate_204");

            if (isEmbed) {
                return true;
            }

            if (host.endsWith(".googlevideo.com") || host.endsWith(".ytimg.com")) {
                return true;
            }

            Log.w(TAG, "BLOCKED YouTube website navigation: " + url);
            return false;
        }

        // 3. Google Services
        if (host.equals("google.com") || host.endsWith(".google.com") ||
            host.contains("google.com.") ||
            host.endsWith(".googleapis.com") ||
            host.endsWith(".gstatic.com") ||
            host.endsWith(".googleusercontent.com") ||
            host.endsWith(".googlevideo.com") ||
            host.endsWith(".ytimg.com") ||
            host.endsWith(".recaptcha.net") || host.equals("recaptcha.net") ||
            host.endsWith(".firebaseapp.com") ||
            host.endsWith(".firebaseio.com") ||
            host.endsWith(".web.app")) {

            if ((host.equals("www.google.com") || host.equals("google.com")) && path.startsWith("/search")) {
                Log.w(TAG, "BLOCKED Google Web Search: " + url);
                return false;
            }

            return true;
        }

        // 4. English Boli (Course)
        if (host.equals("englishboli.com") || host.endsWith(".englishboli.com")) {
            if (isBlogUrl(url, path)) {
                Log.w(TAG, "BLOCKED EnglishBoli Blog: " + url);
                return false;
            }
            return true;
        }

        // 5. Ononno (Online Learning Platform)
        if (host.equals("ononno.com") || host.endsWith(".ononno.com") ||
            host.equals("fahimabdullah.com") || host.endsWith(".fahimabdullah.com")) {
            return true;
        }

        // 6. Media & CDN Infrastructure (Bunny Stream, Klasio, S3 Bucket)
        if (host.equals("cdn.klasio.com") || host.endsWith(".klasio.com") ||
            host.equals("player.mediadelivery.net") || host.endsWith(".mediadelivery.net") ||
            host.endsWith(".b-cdn.net") ||
            host.endsWith(".s3.amazonaws.com") ||
            host.endsWith(".s3.us-east-1.amazonaws.com") ||
            host.endsWith(".cloudflareinsights.com")) {
            return true;
        }

        Log.w(TAG, "BLOCKED outside URL: " + url);
        return false;
    }

    /**
     * Checks if a URL points to the Blog section or blog articles on EnglishBoli.
     */
    private boolean isBlogUrl(String url, String path) {
        if (url == null && path == null) return false;
        String lowerUrl = url != null ? url.toLowerCase() : "";
        String lowerPath = path != null ? path.toLowerCase() : "";

        // Never block valid course / lesson / tutor LMS / auth routes
        if (lowerPath.startsWith("/courses") || 
            lowerPath.startsWith("/course-category") || 
            lowerPath.startsWith("/lesson") || 
            lowerPath.startsWith("/dashboard") ||
            lowerPath.startsWith("/wp-admin/admin-ajax.php") ||
            lowerPath.startsWith("/cart") ||
            lowerPath.startsWith("/checkout")) {
            return false;
        }

        return lowerPath.startsWith("/blog") ||
               lowerPath.equals("/blog") ||
               lowerPath.contains("/blog/") ||
               lowerPath.startsWith("/elementor-instructor") ||
               lowerPath.startsWith("/category/blog") ||
               lowerPath.startsWith("/category/") ||
               lowerPath.startsWith("/tag/") ||
               lowerPath.contains("story-about-design") ||
               lowerPath.contains("how-did-we-get-1m-visitors") ||
               lowerPath.contains("how-to-blow-through-capital") ||
               lowerPath.contains("godfather-ipsum") ||
               lowerPath.contains("only-dont-tell-me-youre-innocent") ||
               lowerPath.contains("ask-your-friends-in-the-neighbourhood") ||
               lowerPath.contains("you-broke-my-heart") ||
               lowerUrl.contains("englishboli.com/blog");
    }

    private void showBlockedNotice(String url) {
        String msg;
        if (url != null && (url.contains("/blog") || url.contains("/elementor-instructor"))) {
            msg = "Access Denied: The Blog section is blocked.";
        } else {
            msg = "Access Denied: Only Lim Edu approved sites can be opened.";
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // WebViewClient: Navigation guard, YouTube embed injection, and PDF detection
    private class LimEduWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            // Direct PDF download detection
            if (url.toLowerCase().endsWith(".pdf") || url.toLowerCase().contains("exercise-files") || url.toLowerCase().contains("download=")) {
                downloadFile(url, cachedUserAgent, null, "application/pdf");
                return true;
            }

            if (isAllowedUrl(url)) {
                return false;
            } else {
                showBlockedNotice(url);
                return true;
            }
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            String tab = (String) view.getTag();
            saveLastUrl(tab, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.w(TAG, "WebView error: " + errorCode + " - " + description + " for " + failingUrl);
            String tab = (String) view.getTag();
            if ("course".equals(tab) && failingUrl != null && !failingUrl.equals(URL_COURSE)) {
                view.loadUrl(URL_COURSE);
            }
        }

        /**
         * Safely fetches network resources with gzip support, redirect handling, and cookie forwarding.
         * Runs on worker thread without touching WebView methods.
         */
        private byte[] fetchNetworkResource(String urlStr, Map<String, String> extraHeaders) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", cachedUserAgent);
                conn.setRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("Referer", "https://englishboli.com/");

                if (extraHeaders != null) {
                    for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                        String key = entry.getKey();
                        if (!"Accept-Encoding".equalsIgnoreCase(key) && !"Host".equalsIgnoreCase(key)) {
                            conn.setRequestProperty(key, entry.getValue());
                        }
                    }
                }

                try {
                    String cookie = CookieManager.getInstance().getCookie(urlStr);
                    if (cookie != null && !cookie.isEmpty()) {
                        conn.setRequestProperty("Cookie", cookie);
                    }
                } catch (Exception ignored) {}

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 303) {
                    String newUrl = conn.getHeaderField("Location");
                    if (newUrl != null && !newUrl.isEmpty()) {
                        conn.disconnect();
                        return fetchNetworkResource(newUrl, extraHeaders);
                    }
                }

                if (code >= 400) {
                    return null;
                }

                InputStream in = conn.getInputStream();
                String encoding = conn.getContentEncoding();
                if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                    in = new GZIPInputStream(in);
                }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] temp = new byte[4096];
                int read;
                while ((read = in.read(temp)) != -1) {
                    buffer.write(temp, 0, read);
                }
                in.close();
                return buffer.toByteArray();
            } catch (Exception e) {
                Log.w(TAG, "Network fetch error: " + urlStr, e);
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request == null || request.getUrl() == null) {
                return super.shouldInterceptRequest(view, request);
            }
            String url = request.getUrl().toString();

            // Strict Main Frame Guard: Block direct or iframe loading of forbidden pages
            if (request.isForMainFrame() && !isAllowedUrl(url)) {
                String siteName = "EnglishBoli";
                String redirectUrl = "https://englishboli.com";
                String reason = "This section has been restricted in Lim Edu.";
                if (url.contains("/blog")) {
                    reason = "The Blog section has been restricted in Lim Edu.";
                }
                String blockedHtml = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>body{background:#0b0f19;color:#fff;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;padding:24px;box-sizing:border-box;text-align:center;}h2{color:#f85149;margin-bottom:12px;font-size:22px;}p{color:#8b949e;line-height:1.6;font-size:15px;margin-bottom:20px;}button{background:#1f6feb;color:#fff;border:none;padding:12px 24px;border-radius:8px;font-size:15px;font-weight:600;cursor:pointer;outline:none;}button:active{opacity:0.8;}</style></head><body><div><h2>Access Blocked</h2><p>" + reason + "</p><button onclick=\"window.location.href='" + redirectUrl + "'\">Go to " + siteName + "</button></div></body></html>";
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "text/html; charset=UTF-8");
                return new WebResourceResponse("text/html", "UTF-8", 200, "OK", headers, new ByteArrayInputStream(blockedHtml.getBytes(StandardCharsets.UTF_8)));
            }

            // 1. Intercept YouTube player base.js
            if ((url.contains("youtube.com/s/player/") || url.contains("youtube-nocookie.com/s/player/")) && url.contains("base.js")) {
                byte[] jsBytes = fetchNetworkResource(url, request.getRequestHeaders());
                if (jsBytes != null) {
                    try {
                        String originalJs = new String(jsBytes, StandardCharsets.UTF_8);
                        String cleanCssQuoted = JSONObject.quote(YOUTUBE_CLEAN_CSS);
                        String injectedJs = "\n\n/* Lim Edu Injected Cleanup */\n" +
                                ";(function() {\n" +
                                "  var css = " + cleanCssQuoted + ";\n" +
                                "  function applyFix() {\n" +
                                "    try {\n" +
                                "      if (!document.getElementById('limedu-injected-css')) {\n" +
                                "        var style = document.createElement('style');\n" +
                                "        style.id = 'limedu-injected-css';\n" +
                                "        style.type = 'text/css';\n" +
                                "        style.textContent = css;\n" +
                                "        (document.head || document.documentElement).appendChild(style);\n" +
                                "      }\n" +
                                "    } catch(e) {}\n" +
                                "    try {\n" +
                                "      var badSels = [" + YOUTUBE_CLEAN_SELECTORS + "];\n" +
                                "      for (var i = 0; i < badSels.length; i++) {\n" +
                                "        var nodes = document.querySelectorAll(badSels[i]);\n" +
                                "        for (var j = 0; j < nodes.length; j++) {\n" +
                                "          nodes[j].remove();\n" +
                                "        }\n" +
                                "      }\n" +
                                "    } catch(e) {}\n" +
                                "  }\n" +
                                "  setInterval(applyFix, 400);\n" +
                                "  applyFix();\n" +
                                "  window.addEventListener('DOMContentLoaded', applyFix);\n" +
                                "  window.addEventListener('load', applyFix);\n" +
                                "})();\n";

                        String combinedJs = originalJs + injectedJs;
                        InputStream modifiedStream = new ByteArrayInputStream(combinedJs.getBytes(StandardCharsets.UTF_8));
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Access-Control-Allow-Origin", "*");
                        headers.put("Content-Type", "application/javascript; charset=UTF-8");
                        return new WebResourceResponse("application/javascript", "UTF-8", 200, "OK", headers, modifiedStream);
                    } catch (Exception e) {
                        Log.w(TAG, "JS mod error", e);
                    }
                }
            }

            // 2. Intercept YouTube CSS files
            if ((url.contains("youtube.com/s/player/") || url.contains("youtube-nocookie.com/s/player/")) && url.contains(".css")) {
                byte[] cssBytes = fetchNetworkResource(url, request.getRequestHeaders());
                if (cssBytes != null) {
                    try {
                        String originalCss = new String(cssBytes, StandardCharsets.UTF_8);
                        String combinedCss = originalCss + "\n\n/* Lim Edu Override */\n" + YOUTUBE_CLEAN_CSS;
                        InputStream modifiedStream = new ByteArrayInputStream(combinedCss.getBytes(StandardCharsets.UTF_8));
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Access-Control-Allow-Origin", "*");
                        headers.put("Content-Type", "text/css; charset=UTF-8");
                        return new WebResourceResponse("text/css", "UTF-8", 200, "OK", headers, modifiedStream);
                    } catch (Exception e) {
                        Log.w(TAG, "CSS mod error", e);
                    }
                }
            }

            // 3. Intercept YouTube embed HTML document (Inject cleanup + lightweight video progress tracker)
            if (url.contains("youtube.com/embed/") || url.contains("youtube-nocookie.com/embed/")) {
                byte[] htmlBytes = fetchNetworkResource(url, request.getRequestHeaders());
                if (htmlBytes != null) {
                    try {
                        String html = new String(htmlBytes, StandardCharsets.UTF_8);

                        String nonce = "";
                        Matcher matcher = Pattern.compile("nonce=\"([^\"]+)\"").matcher(html);
                        if (matcher.find()) {
                            nonce = matcher.group(1);
                        }
                        String nonceAttr = nonce.isEmpty() ? "" : " nonce=\"" + nonce + "\"";

                        // Extract video ID and saved timestamp directly from SharedPreferences
                        String vIdParam = "";
                        int embedIdx = url.indexOf("/embed/");
                        if (embedIdx != -1) {
                            vIdParam = url.substring(embedIdx + 7).split("[?&#/]")[0];
                        }
                        int prefSavedSec = 0;
                        if (!vIdParam.isEmpty()) {
                            try {
                                prefSavedSec = (int) getSharedPreferences(PREF_VIDEO_TIMES, MODE_PRIVATE).getFloat("v_" + vIdParam, 0f);
                            } catch (Exception ignored) {}
                        }

                        // Lightweight, zero-battery-drain tracker: event-driven, throttled to 2.5s intervals during playback
                        String ytTrackerJs =
                                "(function() {" +
                                "  var badSels = [" + YOUTUBE_CLEAN_SELECTORS + "];" +
                                "  function purge() {" +
                                "    try {" +
                                "      for (var i = 0; i < badSels.length; i++) {" +
                                "        var nodes = document.querySelectorAll(badSels[i]);" +
                                "        for (var j = 0; j < nodes.length; j++) { nodes[j].remove(); }" +
                                "      }" +
                                "    } catch(e) {}" +
                                "  }" +
                                "  purge();" +
                                "  var pName = window.location.pathname || '';" +
                                "  var idx = pName.indexOf('/embed/');" +
                                "  var videoId = (idx !== -1) ? pName.substring(idx + 7).split('/')[0] : '';" +
                                "  var storageKey = videoId ? ('limedu_vt_' + videoId) : '';" +
                                "  var prefSaved = " + prefSavedSec + ";" +
                                "  var lastSavedSec = -1;" +
                                "  function saveProgress(curTime, duration) {" +
                                "    if (!videoId || curTime < 0) return;" +
                                "    var sec = Math.floor(curTime);" +
                                "    if (Math.abs(sec - lastSavedSec) < 2) return;" +
                                "    lastSavedSec = sec;" +
                                "    if (duration > 10 && curTime >= duration - 3) { sec = 0; }" +
                                "    try { if (storageKey) localStorage.setItem(storageKey, String(sec)); } catch(e) {}" +
                                "    try { window.parent.postMessage({ type: 'limedu_yt_time', videoId: videoId, time: sec }, '*'); } catch(e) {}" +
                                "    try { if (window.LimEduBridge && window.LimEduBridge.saveVideoTime) { window.LimEduBridge.saveVideoTime(videoId, sec); } } catch(e) {}" +
                                "  }" +
                                "  var attached = false;" +
                                "  function bindVideo() {" +
                                "    if (attached) return;" +
                                "    var v = document.querySelector('video');" +
                                "    if (!v) return;" +
                                "    attached = true;" +
                                "    var target = 0;" +
                                "    try {" +
                                "      var sp = new URLSearchParams(window.location.search);" +
                                "      var st = sp.get('start');" +
                                "      if (st) target = parseFloat(st);" +
                                "    } catch(e) {}" +
                                "    if (target <= 0 && prefSaved > 2) {" +
                                "      target = prefSaved;" +
                                "    } else if (target <= 0 && storageKey) {" +
                                "      try {" +
                                "        var sVal = localStorage.getItem(storageKey);" +
                                "        if (sVal) target = parseFloat(sVal);" +
                                "      } catch(e) {}" +
                                "    }" +
                                "    if (target > 2) {" +
                                "      try {" +
                                "        if (Math.abs(v.currentTime - target) > 2) {" +
                                "          v.currentTime = target;" +
                                "        }" +
                                "      } catch(e) {}" +
                                "    }" +
                                "    v.addEventListener('pause', function() { saveProgress(v.currentTime, v.duration || 0); });" +
                                "    v.addEventListener('seeked', function() { saveProgress(v.currentTime, v.duration || 0); });" +
                                "    v.addEventListener('ended', function() { saveProgress(0, v.duration || 0); });" +
                                "    var lastUpdate = 0;" +
                                "    v.addEventListener('timeupdate', function() {" +
                                "      var now = Date.now();" +
                                "      if (now - lastUpdate >= 2500) {" +
                                "        lastUpdate = now;" +
                                "        saveProgress(v.currentTime, v.duration || 0);" +
                                "      }" +
                                "    });" +
                                "  }" +
                                "  window.addEventListener('DOMContentLoaded', bindVideo);" +
                                "  window.addEventListener('load', bindVideo);" +
                                "  var pollCount = 0;" +
                                "  var pollTimer = setInterval(function() {" +
                                "    pollCount++;" +
                                "    purge();" +
                                "    bindVideo();" +
                                "    if (attached && pollCount > 8) { clearInterval(pollTimer); }" +
                                "  }, 400);" +
                                "})();";

                        String injection = "<style" + nonceAttr + " id=\"limedu-clean-style\">" + YOUTUBE_CLEAN_CSS + "</style>" +
                                "<script" + nonceAttr + ">" + ytTrackerJs + "</script>";

                        if (html.contains("</head>")) {
                            html = html.replace("</head>", injection + "</head>");
                        } else {
                            html = injection + html;
                        }

                        InputStream modifiedIn = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Access-Control-Allow-Origin", "*");
                        headers.put("Content-Type", "text/html; charset=UTF-8");
                        return new WebResourceResponse("text/html", "UTF-8", 200, "OK", headers, modifiedIn);
                    } catch (Exception e) {
                        Log.w(TAG, "HTML mod error", e);
                    }
                }
            }

            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (!isAllowedUrl(url)) {
                showBlockedNotice(url);
            }
            if (view == getActiveWebView()) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (view == getActiveWebView()) {
                progressBar.setVisibility(View.GONE);
                if (appTitle != null && url != null) {
                    if (url.contains("ononno.com")) {
                        appTitle.setText("Ononno");
                    } else if (url.contains("englishboli.com")) {
                        appTitle.setText("EnglishBoli");
                    } else if (url.contains("mail.google.com")) {
                        appTitle.setText("Gmail");
                    } else if (url.contains("keep.google.com")) {
                        appTitle.setText("Keep");
                    } else if (url.contains("accounts.google.com")) {
                        appTitle.setText("Google Account");
                    }
                }
            }
            CookieManager.getInstance().flush();

            String tab = (String) view.getTag();
            saveLastUrl(tab, url);

            // 1. Safe responsive layout & full viewport fit + hide Blog links completely
            String responsiveFix = "javascript:(function() {" +
                    "if (!document.getElementById('limedu-responsive-fix')) {" +
                    "  var style = document.createElement('style');" +
                    "  style.id = 'limedu-responsive-fix';" +
                    "  style.type = 'text/css';" +
                    "  style.innerHTML = 'html, body { width: 100% !important; max-width: 100vw !important; margin: 0 !important; -webkit-text-size-adjust: 100% !important; } " +
                    "body { overflow-x: clip !important; } " +
                    "img, video, iframe { max-width: 100% !important; } " +
                    "iframe[src*=\"youtube\"] { width: 100% !important; aspect-ratio: 16/9; height: auto !important; } " +
                    "#menu-item-7409, a[href*=\"/blog\"], a[href*=\"/elementor-instructor\"], li:has(a[href*=\"/blog\"]), li:has(a[href*=\"elementor-instructor\"]), .sub-menu li:has(a[href*=\"blog\"]) { display: none !important; visibility: hidden !important; pointer-events: none !important; height: 0 !important; margin: 0 !important; padding: 0 !important; }';" +
                    "  document.head.appendChild(style);" +
                    "}" +
                    "function purgeBlog() {" +
                    "  try {" +
                    "    var b = document.querySelectorAll('#menu-item-7409, a[href*=\"/blog\"], a[href*=\"/elementor-instructor\"]');" +
                    "    for (var i = 0; i < b.length; i++) {" +
                    "      var li = b[i].closest('li');" +
                    "      if (li) { li.remove(); } else { b[i].remove(); }" +
                    "    }" +
                    "  } catch(e) {}" +
                    "}" +
                    "purgeBlog();" +
                    "if (!window.__limedu_blog_purger) {" +
                    "  window.__limedu_blog_purger = setInterval(purgeBlog, 600);" +
                    "}" +
                    "})()";
            view.evaluateJavascript(responsiveFix, null);

            // 2. Comprehensive Video Timestamp Restorer & SPA Navigation Tracker
            String trackerScript = "javascript:(function() {" +
                    "var tabTag = '" + (tab != null ? tab : "") + "';" +
                    "function syncUrl() {" +
                    "  try {" +
                    "    var href = window.location.href;" +
                    "    if (href && window.LimEduBridge && window.LimEduBridge.savePageUrl) {" +
                    "      window.LimEduBridge.savePageUrl(tabTag, href);" +
                    "    }" +
                    "  } catch(e) {}" +
                    "}" +
                    "syncUrl();" +
                    "if (!window.__limedu_spa_hooked) {" +
                    "  window.__limedu_spa_hooked = true;" +
                    "  try {" +
                    "    var origPush = history.pushState;" +
                    "    if (origPush) {" +
                    "      history.pushState = function() {" +
                    "        var r = origPush.apply(this, arguments);" +
                    "        syncUrl();" +
                    "        setTimeout(scanAndTrackVideos, 350);" +
                    "        return r;" +
                    "      };" +
                    "    }" +
                    "    var origReplace = history.replaceState;" +
                    "    if (origReplace) {" +
                    "      history.replaceState = function() {" +
                    "        var r = origReplace.apply(this, arguments);" +
                    "        syncUrl();" +
                    "        setTimeout(scanAndTrackVideos, 350);" +
                    "        return r;" +
                    "      };" +
                    "    }" +
                    "  } catch(e) {}" +
                    "  window.addEventListener('popstate', function() { syncUrl(); setTimeout(scanAndTrackVideos, 350); });" +
                    "  window.addEventListener('hashchange', function() { syncUrl(); setTimeout(scanAndTrackVideos, 350); });" +
                    "}" +
                    "function getVideoKey(el, isIframe) {" +
                    "  var existing = el.getAttribute('data-limedu-key');" +
                    "  if (existing) return existing;" +
                    "  var key = '';" +
                    "  if (isIframe) {" +
                    "    var s = el.src || el.getAttribute('data-src') || '';" +
                    "    var ym = s.match(/(?:embed\\/|v=|vi\\/|youtu\\.be\\/|\\/v\\/)([a-zA-Z0-9_-]{11})/);" +
                    "    if (ym) { key = ym[1]; }" +
                    "    else {" +
                    "      var vm = s.match(/player\\.vimeo\\.com\\/video\\/([0-9]+)/);" +
                    "      if (vm) { key = 'vimeo_' + vm[1]; }" +
                    "      else { key = 'iframe_' + (s.split('?')[0] || window.location.pathname); }" +
                    "    }" +
                    "  } else {" +
                    "    var vid = el.id || el.getAttribute('data-id') || el.getAttribute('data-video-id') || el.getAttribute('data-lesson-id');" +
                    "    if (vid) { key = 'v_' + vid; }" +
                    "    else {" +
                    "      var cSrc = el.currentSrc || el.src || '';" +
                    "      if (cSrc && cSrc.indexOf('blob:') === -1) {" +
                    "        var clean = cSrc.split('?')[0].replace(/[^a-zA-Z0-9_-]/g, '_');" +
                    "        key = 'vsrc_' + (clean.length > 32 ? clean.substring(clean.length - 32) : clean);" +
                    "      } else {" +
                    "        var pName = (window.location.pathname || 'page').replace(/[^a-zA-Z0-9_-]/g, '_');" +
                    "        key = 'vpath_' + pName;" +
                    "      }" +
                    "    }" +
                    "  }" +
                    "  el.setAttribute('data-limedu-key', key);" +
                    "  return key;" +
                    "}" +
                    "function bindHtml5Video(v) {" +
                    "  if (!v || v.__limedu_bound) return;" +
                    "  v.__limedu_bound = true;" +
                    "  var key = getVideoKey(v, false);" +
                    "  if (!key) return;" +
                    "  var savedTime = 0;" +
                    "  try {" +
                    "    if (window.LimEduBridge && window.LimEduBridge.getVideoTime) {" +
                    "      savedTime = window.LimEduBridge.getVideoTime(key);" +
                    "    }" +
                    "  } catch(e) {}" +
                    "  function restoreTime() {" +
                    "    if (savedTime > 2 && Math.abs(v.currentTime - savedTime) > 1.5) {" +
                    "      try { v.currentTime = savedTime; } catch(e) {}" +
                    "    }" +
                    "  }" +
                    "  if (savedTime > 2) {" +
                    "    if (v.readyState >= 1) { restoreTime(); }" +
                    "    else {" +
                    "      v.addEventListener('loadedmetadata', restoreTime, { once: true });" +
                    "      v.addEventListener('canplay', restoreTime, { once: true });" +
                    "    }" +
                    "  }" +
                    "  var lastSave = -1;" +
                    "  function saveProgress() {" +
                    "    try {" +
                    "      var t = v.currentTime;" +
                    "      if (t < 0) return;" +
                    "      var sec = Math.floor(t);" +
                    "      if (v.duration && t >= v.duration - 3) { sec = 0; }" +
                    "      if (Math.abs(sec - lastSave) < 2) return;" +
                    "      lastSave = sec;" +
                    "      if (window.LimEduBridge && window.LimEduBridge.saveVideoTime) {" +
                    "        window.LimEduBridge.saveVideoTime(key, sec);" +
                    "      }" +
                    "    } catch(e) {}" +
                    "  }" +
                    "  v.addEventListener('timeupdate', function() {" +
                    "    var now = Date.now();" +
                    "    if (!v.__lastTimeUpdate || now - v.__lastTimeUpdate >= 2000) {" +
                    "      v.__lastTimeUpdate = now;" +
                    "      saveProgress();" +
                    "    }" +
                    "  });" +
                    "  v.addEventListener('pause', saveProgress);" +
                    "  v.addEventListener('seeked', saveProgress);" +
                    "  v.addEventListener('ended', function() {" +
                    "    if (window.LimEduBridge && window.LimEduBridge.saveVideoTime) {" +
                    "      window.LimEduBridge.saveVideoTime(key, 0);" +
                    "    }" +
                    "  });" +
                    "}" +
                    "function bindIframe(f) {" +
                    "  if (!f) return;" +
                    "  var s = f.src || f.getAttribute('data-src') || '';" +
                    "  if (!s) return;" +
                    "  var isYt = s.indexOf('youtube.com') !== -1 || s.indexOf('youtube-nocookie.com') !== -1;" +
                    "  var isVimeo = s.indexOf('player.vimeo.com') !== -1;" +
                    "  if (!isYt && !isVimeo) return;" +
                    "  var key = getVideoKey(f, true);" +
                    "  if (!key || f.__limedu_iframe_bound) return;" +
                    "  f.__limedu_iframe_bound = true;" +
                    "  var savedTime = 0;" +
                    "  try {" +
                    "    if (window.LimEduBridge && window.LimEduBridge.getVideoTime) {" +
                    "      savedTime = Math.floor(window.LimEduBridge.getVideoTime(key));" +
                    "      if (savedTime <= 2 && isYt) {" +
                    "        savedTime = Math.floor(window.LimEduBridge.getVideoTime('yt_' + key));" +
                    "      }" +
                    "    }" +
                    "  } catch(e) {}" +
                    "  if (savedTime > 2) {" +
                    "    try {" +
                    "      var u = new URL(s);" +
                    "      var mod = false;" +
                    "      if (isYt) {" +
                    "        if (u.searchParams.get('start') !== String(savedTime)) {" +
                    "          u.searchParams.set('start', String(savedTime));" +
                    "          mod = true;" +
                    "        }" +
                    "        if (u.searchParams.get('enablejsapi') !== '1') { u.searchParams.set('enablejsapi', '1'); mod = true; }" +
                    "        if (u.searchParams.get('iv_load_policy') !== '3') { u.searchParams.set('iv_load_policy', '3'); mod = true; }" +
                    "        if (u.searchParams.get('modestbranding') !== '1') { u.searchParams.set('modestbranding', '1'); mod = true; }" +
                    "        if (u.searchParams.get('rel') !== '0') { u.searchParams.set('rel', '0'); mod = true; }" +
                    "      } else if (isVimeo) {" +
                    "        if (!u.hash || u.hash.indexOf('#t=') === -1) {" +
                    "          u.hash = '#t=' + savedTime + 's';" +
                    "          mod = true;" +
                    "        }" +
                    "      }" +
                    "      if (mod) { f.src = u.href; }" +
                    "    } catch(e) {}" +
                    "  }" +
                    "}" +
                    "function scanAndTrackVideos() {" +
                    "  try {" +
                    "    var vList = document.querySelectorAll('video');" +
                    "    for (var i = 0; i < vList.length; i++) { bindHtml5Video(vList[i]); }" +
                    "    var fList = document.querySelectorAll('iframe');" +
                    "    for (var j = 0; j < fList.length; j++) { bindIframe(fList[j]); }" +
                    "  } catch(e) {}" +
                    "}" +
                    "scanAndTrackVideos();" +
                    "if (!window.__limedu_obs_installed) {" +
                    "  window.__limedu_obs_installed = true;" +
                    "  var obs = new MutationObserver(function() { scanAndTrackVideos(); });" +
                    "  obs.observe(document.body || document.documentElement, { childList: true, subtree: true });" +
                    "  setInterval(function() { syncUrl(); scanAndTrackVideos(); }, 1500);" +
                    "  window.addEventListener('message', function(e) {" +
                    "    try {" +
                    "      if (e.data && e.data.type === 'limedu_yt_time') {" +
                    "        if (window.LimEduBridge && window.LimEduBridge.saveVideoTime) {" +
                    "          window.LimEduBridge.saveVideoTime(e.data.videoId, e.data.time);" +
                    "          window.LimEduBridge.saveVideoTime('yt_' + e.data.videoId, e.data.time);" +
                    "        }" +
                    "      }" +
                    "    } catch(err) {}" +
                    "  });" +
                    "}" +
                    "})()";
            view.evaluateJavascript(trackerScript, null);
        }
    }

    // WebChromeClient: Fullscreen video, progress bar, alert/confirm dialogs, & OAuth popup handler
    private class LimEduWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (view == getActiveWebView()) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Confirmation")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                    .setOnCancelListener(dialog -> result.cancel())
                    .show();
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Notice")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setOnCancelListener(dialog -> result.cancel())
                    .show();
            return true;
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            WebView tempWebView = new WebView(MainActivity.this);
            tempWebView.getSettings().setJavaScriptEnabled(true);
            tempWebView.getSettings().setDomStorageEnabled(true);
            tempWebView.getSettings().setUserAgentString(cachedUserAgent);

            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(tempWebView, true);

            tempWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                    String targetUrl = request.getUrl().toString();
                    if (isAllowedUrl(targetUrl)) {
                        view.loadUrl(targetUrl);
                    } else {
                        showBlockedNotice(targetUrl);
                    }
                    return true;
                }
            });

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(tempWebView);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customVideoView != null) {
                onHideCustomView();
                return;
            }

            customVideoView = view;
            customViewCallback = callback;

            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            webviewContainer.setVisibility(View.GONE);

            fullscreenContainer.addView(customVideoView);
            fullscreenContainer.setVisibility(View.VISIBLE);

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            hideSystemBars();
        }

        @Override
        public void onHideCustomView() {
            exitVideoFullscreen();
        }
    }

    private void exitVideoFullscreen() {
        if (customVideoView == null) return;

        fullscreenContainer.removeView(customVideoView);
        fullscreenContainer.setVisibility(View.GONE);
        customVideoView = null;

        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }

        if (webviewContainer != null) {
            webviewContainer.setVisibility(View.VISIBLE);
        }
        if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
        if (topBar != null && !isManualFullscreen) topBar.setVisibility(View.VISIBLE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        showSystemBars();
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebView active = getActiveWebView();
        if (active != null) {
            active.onResume();
            String current = active.getUrl();
            if (current != null && current.contains("chromewebdata")) {
                showHomeScreen();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveActiveUrls();
        pauseAllWebViews();
    }

    private void saveActiveUrls() {
        if (webViewCourse != null && webViewCourse.getUrl() != null) {
            saveLastUrl("course", webViewCourse.getUrl());
        }
        if (webViewOnonno != null && webViewOnonno.getUrl() != null) {
            saveLastUrl("ononno", webViewOnonno.getUrl());
        }
        if (webViewGmail != null && webViewGmail.getUrl() != null) {
            saveLastUrl("gmail", webViewGmail.getUrl());
        }
        if (webViewKeep != null && webViewKeep.getUrl() != null) {
            saveLastUrl("keep", webViewKeep.getUrl());
        }
        if (webViewAccount != null && webViewAccount.getUrl() != null) {
            saveLastUrl("account", webViewAccount.getUrl());
        }
    }

    @Override
    protected void onDestroy() {
        if (webViewCourse != null) webViewCourse.destroy();
        if (webViewOnonno != null) webViewOnonno.destroy();
        if (webViewGmail != null) webViewGmail.destroy();
        if (webViewKeep != null) webViewKeep.destroy();
        if (webViewAccount != null) webViewAccount.destroy();
        super.onDestroy();
    }
}
