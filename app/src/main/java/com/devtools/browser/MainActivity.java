package com.devtools.browser;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput, consoleInput;
    private LinearLayout devToolsDrawer, consoleLogsLayout;
    private ScrollView consoleScrollView;
    private Button btnGo, btnToggleDevTools, btnRunConsole, btnInjectBlocker;
    private boolean isDevToolsOpen = false;

    private static final String ANTI_POPUP_SCRIPT = 
            "(function() {" +
            "  window.open = function() { console.log('[Chặn] Đã ngăn mở tab/popup mới!'); return null; };" +
            "  document.querySelectorAll(\"a[target='_blank']\").forEach(a => a.target = '_self');" +
            "  const observer = new MutationObserver(mutations => {" +
            "    mutations.forEach(m => m.addedNodes.forEach(n => {" +
            "      if (n.nodeType === 1) {" +
            "        if (n.tagName === 'A' && n.getAttribute('target') === '_blank') n.setAttribute('target', '_self');" +
            "        if (n.querySelectorAll) n.querySelectorAll(\"a[target='_blank']\").forEach(a => a.setAttribute('target', '_self'));" +
            "      }" +
            "    }));" +
            "  });" +
            "  observer.observe(document.body, { childList: true, subtree: true });" +
            "  const adSelectors = [\"iframe[src*='ads']\", \"iframe[src*='doubleclick']\", \"div[id*='pop']\", \"div[class*='pop']\", \"div[id*='ad']\", \"div[class*='ad']\", \".popunder\", \".popup\"];" +
            "  function hideAds() { adSelectors.forEach(s => document.querySelectorAll(s).forEach(el => { if(el.offsetWidth > 0 || el.offsetHeight > 0) el.style.display = 'none'; })); }" +
            "  hideAds(); setInterval(hideAds, 2000);" +
            "  console.log('[DevTools] Đã bật chặn Quảng Cáo & Nhảy Tab!');" +
            "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        consoleInput = findViewById(R.id.consoleInput);
        devToolsDrawer = findViewById(R.id.devToolsDrawer);
        consoleLogsLayout = findViewById(R.id.consoleLogsLayout);
        consoleScrollView = findViewById(R.id.consoleScrollView);
        btnGo = findViewById(R.id.btnGo);
        btnToggleDevTools = findViewById(R.id.btnToggleDevTools);
        btnRunConsole = findViewById(R.id.btnRunConsole);
        btnInjectBlocker = findViewById(R.id.btnInjectBlocker);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                urlInput.setText(url);
                appendLog("info", "Đang tải: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                appendLog("info", "Tải trang hoàn tất!");
                webView.evaluateJavascript(ANTI_POPUP_SCRIPT, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String level = consoleMessage.messageLevel().name().toLowerCase();
                String msg = "[" + consoleMessage.lineNumber() + "] " + consoleMessage.message();
                appendLog(level, msg);
                return true;
            }
        });

        btnGo.setOnClickListener(v -> {
            String input = urlInput.getText().toString().trim();
            String targetUrl;
            
            if (input.startsWith("http://") || input.startsWith("https://")) {
                targetUrl = input;
            } else if (input.contains(".") && !input.contains(" ")) {
                targetUrl = "https://" + input;
            } else {
                // Nếu là từ khóa, tự động chuyển sang tìm kiếm Google
                targetUrl = "https://www.google.com/search?q=" + Uri.encode(input);
            }
            
            webView.loadUrl(targetUrl);
        });

        btnToggleDevTools.setOnClickListener(v -> {
            isDevToolsOpen = !isDevToolsOpen;
            devToolsDrawer.setVisibility(isDevToolsOpen ? View.VISIBLE : View.GONE);
            btnToggleDevTools.setText(isDevToolsOpen ? "Ẩn F12" : "F12 CONSOLE");
        });

        btnRunConsole.setOnClickListener(v -> {
            String script = consoleInput.getText().toString();
            if (!script.isEmpty()) {
                appendLog("input", "> " + script);
                webView.evaluateJavascript(script, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        appendLog("output", "< " + value);
                    }
                });
                consoleInput.setText("");
            }
        });

        btnInjectBlocker.setOnClickListener(v -> {
            webView.evaluateJavascript(ANTI_POPUP_SCRIPT, null);
            Toast.makeText(this, "Đã kích hoạt kịch bản chặn QC!", Toast.LENGTH_SHORT).show();
        });

        webView.loadUrl("https://www.google.com");
    }

    private void appendLog(String type, String message) {
        TextView logView = new TextView(this);
        logView.setTextSize(12);
        logView.setPadding(8, 4, 8, 4);

        if (type.contains("error")) {
            logView.setTextColor(0xFFFF5252);
        } else if (type.contains("warn")) {
            logView.setTextColor(0xFFFFD700);
        } else if (type.equals("input")) {
            logView.setTextColor(0xFF00E676);
        } else if (type.equals("output")) {
            logView.setTextColor(0xFF40C4FF);
        } else {
            logView.setTextColor(0xFFE0E0E0);
        }

        logView.setText(message);
        consoleLogsLayout.addView(logView);
        consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
