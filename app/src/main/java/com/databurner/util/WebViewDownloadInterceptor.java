package com.databurner.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Off-screen Headless WebView Interceptor.
 * Loads dynamic web pages (e.g. developer.android.com/studio), executes JavaScript,
 * simulates download button interactions, and intercepts direct CDN download URLs
 * via DownloadListener.onDownloadStart and WebViewClient.shouldOverrideUrlLoading in real-time.
 */
public class WebViewDownloadInterceptor {

    public interface OnUrlExtractedListener {
        void onExtracted(String url);
    }

    private static final Pattern BINARY_FILE_PATTERN = Pattern.compile(".*\\.(zip|iso|dmg|exe|bin|apk)($|\\?.*)", Pattern.CASE_INSENSITIVE);

    public static void interceptPageDownloads(Context context, String pageUrl, OnUrlExtractedListener listener) {
        if (context == null || pageUrl == null || pageUrl.trim().isEmpty()) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                final WebView webView = new WebView(context.getApplicationContext());
                final AtomicInteger extractedCount = new AtomicInteger(0);
                final Handler mainHandler = new Handler(Looper.getMainLooper());

                WebSettings settings = webView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setDatabaseEnabled(true);
                settings.setAllowFileAccess(false);
                settings.setLoadWithOverviewMode(true);
                settings.setUseWideViewPort(true);
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

                final Runnable cleanup = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            webView.stopLoading();
                            webView.destroy();
                        } catch (Exception ignored) {}
                    }
                };

                // 15-second watchdog cleanup
                mainHandler.postDelayed(cleanup, 15000);

                OnUrlExtractedListener internalListener = url -> {
                    if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                        if (listener != null) {
                            listener.onExtracted(url);
                        }
                        if (extractedCount.incrementAndGet() >= 6) {
                            mainHandler.removeCallbacks(cleanup);
                            webView.stopLoading();
                            webView.destroy();
                        }
                    }
                };

                webView.setDownloadListener(new DownloadListener() {
                    @Override
                    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                        internalListener.onExtracted(url);
                    }
                });

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        if (request != null && request.getUrl() != null) {
                            String urlStr = request.getUrl().toString();
                            if (BINARY_FILE_PATTERN.matcher(urlStr).matches()) {
                                internalListener.onExtracted(urlStr);
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (url != null && BINARY_FILE_PATTERN.matcher(url).matches()) {
                            internalListener.onExtracted(url);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Runnable triggerJs = () -> {
                            try {
                                String jsScript = "javascript:(function() {" +
                                        "   var links = document.querySelectorAll('a[href], button[data-href], a[data-url]');" +
                                        "   var count = 0;" +
                                        "   for (var i = 0; i < links.length && count < 8; i++) {" +
                                        "       var href = links[i].href || links[i].getAttribute('data-href') || links[i].getAttribute('data-url');" +
                                        "       if (href && href.match(/\\.(zip|iso|dmg|exe|bin|apk)($|\\?)/i)) {" +
                                        "           try { links[i].click(); count++; } catch(e){}" +
                                        "       }" +
                                        "   }" +
                                        "})()";
                                webView.evaluateJavascript(jsScript, null);
                            } catch (Exception ignored) {}
                        };

                        // Immediate execution + 1.5s delayed execution for AJAX rendered links
                        triggerJs.run();
                        mainHandler.postDelayed(triggerJs, 1500);
                    }
                });

                webView.loadUrl(pageUrl);
            } catch (Exception ignored) {}
        });
    }
}
