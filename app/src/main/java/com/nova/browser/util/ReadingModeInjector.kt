package com.nova.browser.util

import android.webkit.WebView

/**
 * ReadingModeInjector
 *
 * Injects a self-contained JavaScript reader into any article page.
 * It heuristically finds the main content block, strips nav/ads/sidebars,
 * then overlays a clean, readable card on top of the page.
 *
 * NO external dependencies — pure JS injected at runtime.
 *
 * Usage:
 *   ReadingModeInjector.inject(webView)   // enter reading mode
 *   ReadingModeInjector.remove(webView)   // exit reading mode
 */
object ReadingModeInjector {

    private const val OVERLAY_ID = "tutu-reader-overlay"

    // Full self-contained reading mode script.
    // 1. Scores candidate content blocks by text density.
    // 2. Picks the winner.
    // 3. Renders a clean overlay with the article title + content.
    private val READER_SCRIPT = """
(function() {
    // --- Guard: don't inject twice ---
    if (document.getElementById('$OVERLAY_ID')) return;

    // --- 1. Extract title ---
    var title =
        document.querySelector('h1')?.innerText ||
        document.querySelector('meta[property="og:title"]')?.content ||
        document.title ||
        'Article';

    // --- 2. Score candidate elements by text density ---
    var candidates = Array.from(
        document.querySelectorAll('article, [role="main"], main, .post-content, .entry-content, .article-body, .content, #content, #main, .story-body, p')
    );

    function scoreElement(el) {
        var text = el.innerText || '';
        var textLen = text.trim().length;
        var linkLen = Array.from(el.querySelectorAll('a'))
            .reduce(function(sum, a) { return sum + (a.innerText || '').length; }, 0);
        var pCount = el.querySelectorAll('p').length;
        return textLen - linkLen * 3 + pCount * 25;
    }

    var best = null;
    var bestScore = 0;

    // First try semantic containers
    var semanticSelectors = ['article', '[role="main"]', 'main', '.post-content',
        '.entry-content', '.article-body', '.story-body'];
    for (var sel of semanticSelectors) {
        var el = document.querySelector(sel);
        if (el) { best = el; break; }
    }

    // Fallback: score all divs with significant text
    if (!best) {
        Array.from(document.querySelectorAll('div')).forEach(function(div) {
            var score = scoreElement(div);
            if (score > bestScore) {
                bestScore = score;
                best = div;
            }
        });
    }

    var content = best ? best.innerHTML : '<p>Could not extract content for this page.</p>';

    // --- 3. Build the overlay ---
    var overlay = document.createElement('div');
    overlay.id = '$OVERLAY_ID';
    overlay.innerHTML = '<div id="tutu-reader-inner">' +
        '<div id="tutu-reader-header">' +
            '<span id="tutu-reader-label">📖 Reading Mode</span>' +
            '<button id="tutu-reader-close" onclick="(function(){' +
                'var o=document.getElementById(\'$OVERLAY_ID\');' +
                'if(o)o.parentNode.removeChild(o);' +
            '})()">✕ Exit</button>' +
        '</div>' +
        '<h1 id="tutu-reader-title">' + title + '</h1>' +
        '<div id="tutu-reader-body">' + content + '</div>' +
    '</div>';

    // --- 4. Style the overlay ---
    var style = document.createElement('style');
    style.textContent = [
        '#$OVERLAY_ID {',
        '  position: fixed; top: 0; left: 0; width: 100%; height: 100%;',
        '  background: #FAFAFA; z-index: 2147483647;',
        '  overflow-y: auto; -webkit-overflow-scrolling: touch;',
        '  box-sizing: border-box; padding: 0;',
        '}',
        '#tutu-reader-inner {',
        '  max-width: 720px; margin: 0 auto;',
        '  padding: 20px 24px 60px 24px; font-family: Georgia, serif;',
        '}',
        '#tutu-reader-header {',
        '  display: flex; justify-content: space-between; align-items: center;',
        '  padding: 12px 0; border-bottom: 1px solid #E0E0E0; margin-bottom: 24px;',
        '}',
        '#tutu-reader-label {',
        '  font-size: 13px; color: #888; font-family: sans-serif; font-style: normal;',
        '}',
        '#tutu-reader-close {',
        '  font-size: 13px; color: #E53935; background: none;',
        '  border: 1px solid #E53935; border-radius: 16px;',
        '  padding: 4px 12px; cursor: pointer; font-family: sans-serif;',
        '}',
        '#tutu-reader-title {',
        '  font-size: 26px; font-weight: bold; line-height: 1.35;',
        '  color: #1A1A1A; margin-bottom: 20px;',
        '}',
        '#tutu-reader-body {',
        '  font-size: 18px; line-height: 1.75; color: #333;',
        '}',
        '#tutu-reader-body p { margin-bottom: 16px; }',
        '#tutu-reader-body h2, #tutu-reader-body h3 {',
        '  font-size: 20px; font-weight: bold; margin: 24px 0 8px; color: #1A1A1A;',
        '}',
        '#tutu-reader-body img {',
        '  max-width: 100%; height: auto; border-radius: 8px; margin: 16px 0;',
        '}',
        '#tutu-reader-body a { color: #E53935; }',
        '#tutu-reader-body ul, #tutu-reader-body ol {',
        '  padding-left: 24px; margin-bottom: 16px;',
        '}',
        '#tutu-reader-body li { margin-bottom: 8px; }',
        '* { box-sizing: border-box; }',
        '@media (prefers-color-scheme: dark) {',
        '  #$OVERLAY_ID { background: #1C1C1E; }',
        '  #tutu-reader-title, #tutu-reader-body h2, #tutu-reader-body h3 { color: #F5F5F5; }',
        '  #tutu-reader-body { color: #D0D0D0; }',
        '  #tutu-reader-header { border-color: #333; }',
        '}'
    ].join('\n');

    overlay.appendChild(style);
    document.body.appendChild(overlay);
})();
""".trimIndent()

    private val REMOVE_SCRIPT = """
        (function() {
            var o = document.getElementById('$OVERLAY_ID');
            if (o) o.parentNode.removeChild(o);
        })();
    """.trimIndent()

    /** Activates reading mode on the current page. */
    fun inject(webView: WebView) {
        webView.evaluateJavascript(READER_SCRIPT, null)
    }

    /** Dismisses reading mode, restoring the original page. */
    fun remove(webView: WebView) {
        webView.evaluateJavascript(REMOVE_SCRIPT, null)
    }

    /**
     * Convenience toggle.
     *
     * Example:
     *   isReadingMode = ReadingModeInjector.toggle(webView, isReadingMode)
     */
    fun toggle(webView: WebView, currentState: Boolean): Boolean {
        return if (currentState) {
            remove(webView)
            false
        } else {
            inject(webView)
            true
        }
    }
}
