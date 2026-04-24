package com.nova.browser.util

import android.util.Log
import android.webkit.JavascriptInterface

/**
 * JavaScript interface for reporting detected media elements back to the app.
 */
class ResourceSnifferJsBridge(
    private val sniffer: ResourceSniffer
) {

    companion object {
        private const val TAG = "ResourceSnifferJs"

        /**
         * JavaScript code injected into every page to detect media elements.
         * Reports findings via window.ResourceSnifferBridge.report().
         */
        fun getInjectionScript(): String = """
            (function() {
                if (window.__novaSnifferInstalled) return;
                window.__novaSnifferInstalled = true;

                const reported = new Set();

                function report(url, type, title) {
                    if (!url || url.startsWith('data:') || url.startsWith('javascript:')) return;
                    if (reported.has(url)) return;
                    reported.add(url);
                    if (window.ResourceSnifferBridge && window.ResourceSnifferBridge.report) {
                        window.ResourceSnifferBridge.report(url, type, title || '');
                    }
                }

                function scanMediaElements() {
                    // Video elements
                    document.querySelectorAll('video').forEach(function(v) {
                        if (v.src) report(v.src, 'video', v.title || document.title);
                        v.querySelectorAll('source').forEach(function(s) {
                            if (s.src) report(s.src, 'video', s.type || '');
                        });
                    });

                    // Audio elements
                    document.querySelectorAll('audio').forEach(function(a) {
                        if (a.src) report(a.src, 'audio', a.title || document.title);
                        a.querySelectorAll('source').forEach(function(s) {
                            if (s.src) report(s.src, 'audio', s.type || '');
                        });
                    });

                    // Image elements (large images only)
                    document.querySelectorAll('img').forEach(function(img) {
                        if (img.src && (img.naturalWidth > 300 || img.width > 300)) {
                            report(img.src, 'image', img.alt || '');
                        }
                    });

                    // Source elements outside video/audio
                    document.querySelectorAll('source[src]').forEach(function(s) {
                        var type = s.type || '';
                        if (type.includes('video')) report(s.src, 'video', '');
                        else if (type.includes('audio')) report(s.src, 'audio', '');
                    });
                }

                // Hook URL.createObjectURL to capture blob URLs
                var originalCreateObjectURL = URL.createObjectURL;
                URL.createObjectURL = function(obj) {
                    var url = originalCreateObjectURL.call(URL, obj);
                    if (obj instanceof Blob || obj instanceof MediaSource) {
                        report(url, 'blob', '');
                    }
                    return url;
                };

                // Hook MediaSource for HLS/DASH streams
                if (window.MediaSource) {
                    var originalAddSourceBuffer = MediaSource.prototype.addSourceBuffer;
                    MediaSource.prototype.addSourceBuffer = function(mimeType) {
                        var sb = originalAddSourceBuffer.call(this, mimeType);
                        if (mimeType.includes('video')) {
                            report(window.URL.createObjectURL ? 'mediasource://' : '', 'video', mimeType);
                        }
                        return sb;
                    };
                }

                // Hook fetch to capture media responses
                var originalFetch = window.fetch;
                window.fetch = function() {
                    var url = arguments[0];
                    if (typeof url === 'string') {
                        var lower = url.toLowerCase();
                        if (lower.includes('.mp4') || lower.includes('.m3u8') || lower.includes('.webm') ||
                            lower.includes('.mp3') || lower.includes('.m4a') || lower.includes('.ts')) {
                            report(url, 'video', '');
                        }
                    }
                    return originalFetch.apply(this, arguments);
                };

                // Hook XMLHttpRequest to capture media URLs
                var originalXhrOpen = XMLHttpRequest.prototype.open;
                XMLHttpRequest.prototype.open = function(method, url) {
                    var lower = (url || '').toString().toLowerCase();
                    if (lower.includes('.mp4') || lower.includes('.m3u8') || lower.includes('.webm') ||
                        lower.includes('.mp3') || lower.includes('.m4a') || lower.includes('.ts') ||
                        lower.includes('.mpd')) {
                        report(url.toString(), 'video', '');
                    }
                    return originalXhrOpen.apply(this, arguments);
                };

                // Scan on initial load and periodically for dynamic content
                scanMediaElements();

                // MutationObserver for dynamically added media
                var observer = new MutationObserver(function(mutations) {
                    var hasMedia = false;
                    mutations.forEach(function(m) {
                        m.addedNodes.forEach(function(node) {
                            if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO' ||
                                node.tagName === 'SOURCE' || node.tagName === 'IMG') {
                                hasMedia = true;
                            }
                        });
                    });
                    if (hasMedia) scanMediaElements();
                });
                observer.observe(document.body || document.documentElement, { childList: true, subtree: true });

                // Also scan periodically for lazy-loaded content
                setInterval(scanMediaElements, 3000);

                // Listen for video play events to catch dynamically loaded src
                document.addEventListener('play', function(e) {
                    var el = e.target;
                    if (el.tagName === 'VIDEO' && el.src) {
                        report(el.src, 'video', el.title || document.title);
                    }
                    if (el.tagName === 'AUDIO' && el.src) {
                        report(el.src, 'audio', el.title || document.title);
                    }
                }, true);

            })();
        """.trimIndent()
    }

    @JavascriptInterface
    fun report(url: String, type: String, title: String) {
        Log.d(TAG, "JS reported: type=$type url=$url")
        sniffer.onJsReport(url, type, title.takeIf { it.isNotBlank() })
    }
}
