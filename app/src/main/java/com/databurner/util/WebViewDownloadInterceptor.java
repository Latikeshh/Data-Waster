package com.databurner.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Off-screen Headless WebView Interceptor.
 * Loads dynamic web pages (e.g. developer.android.com/studio), executes JavaScript,
 * simulates download button interactions, and intercepts multiple direct CDN download URLs
 * via DownloadListener.onDownloadStart in real-time.
 */
public class WebViewDownloadInterceptor {

    public interface OnUrlExtractedListener {
        void onExtracted(String url);
    }

    public static void interceptPageDownloads(Context context, String pageUrl, OnUrlExtractedListener listener) {
        if (context == null || pageUrl == null || pageUrl.trim().isEmpty()) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                final WebView webView = new WebView(context.getApplicationContext());
                final AtomicInteger extractedCount = new AtomicInteger(0);

                WebSettings settings = webView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setAllowFileAccess(false);
                settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

                final Handler mainHandler = new Handler(Looper.getMainLooper());
                final Runnable cleanup = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            webView.stopLoading();
                            webView.destroy();
                        } catch (Exception ignored) {}
                    }
                };

                // 10-second watchdog cleanup
                mainHandler.postDelayed(cleanup, 10000);

                webView.setDownloadListener(new DownloadListener() {
                    @Override
                    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                        try {
                            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                                if (listener != null) {
                                    listener.onExtracted(url);
                                }
                            }
                            // Collect up to 5 distinct download URLs per page before destroying WebView
                            if (extractedCount.incrementAndGet() >= 5) {
                                webView.stopLoading();
                                mainHandler.removeCallbacks(cleanup);
                                webView.destroy();
                            }
                        } catch (Exception ignored) {}
                    }
                });

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        try {
                            // JavaScript loop: Click and trigger all matching binary download links on page
                            String jsScript = "javascript:(function() {" +
                                    "   var links = document.getElementsByTagName('a');" +
                                    "   var count = 0;" +
                                    "   for (var i = 0; i < links.length && count < 5; i++) {" +
                                    "       var href = links[i].href;" +
                                    "       if (href && href.match(/\\.(zip|iso|dmg|exe|bin|apk)$/i)) {" +
                                    "           try { links[i].click(); count++; } catch(e){}" +
                                    "       }" +
                                    "   }" +
                                    "})()";
                            webView.evaluateJavascript(jsScript, null);
                        } catch (Exception ignored) {}
                    }
                });

                webView.loadUrl(pageUrl);
            } catch (Exception ignored) {}
        });
    }
}
