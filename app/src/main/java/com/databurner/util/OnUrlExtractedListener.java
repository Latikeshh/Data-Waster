package com.databurner.util;

/**
 * Listener interface for receiving intercepted live download URLs from WebView.
 */
public interface OnUrlExtractedListener {
    void onExtracted(String url);
}
