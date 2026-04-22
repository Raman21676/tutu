package com.nova.browser.util

import android.webkit.WebView

/**
 * Cosmetic ad filter — the second layer of ad blocking.
 *
 * After network requests are blocked by AdBlocker.shouldBlock(), ad containers
 * can still appear as empty/broken iframes or blank spaces. This injector hides
 * them via CSS selectors and a MutationObserver that catches dynamically added nodes.
 *
 * Does NOT run on TikTok — JS injection breaks TikTok's feed.
 */
object AdBlockCosmeticInjector {

    private fun buildCss(): String = """
        .ad,.ads,.ad-unit,.ad-container,.ad-wrapper,.ad-slot,.ad-banner,
        .ad-block,.adsbygoogle,.advert,.advertisement,.advertising,
        .google-auto-placed,.google_ad_section,.AdContainer,.Ad,
        [class*="ad-unit"],[class*="-ad-"],[class*="_ad_"],
        [class*="adsbygoogle"],[class*="googlead"],
        [id*="google_ads"],[id*="advert"],[id*="ad-container"],
        [id^="ad_"],[id^="div-gpt"],[data-ad-slot],[data-google-av-type],
        ins.adsbygoogle,
        .ytp-ad-module,.ytp-ad-overlay-container,.ytp-ad-text-overlay,
        .ytp-ad-skip-button-container,.ytp-ad-progress-list,
        .video-ads,.ad-showing ytd-player,
        ytd-promoted-video-renderer,ytd-compact-promoted-video-renderer,
        ytd-display-ad-renderer,ytd-statement-banner-renderer,
        ytd-rich-item-renderer:has(ytd-ad-slot-renderer),
        #masthead-ad,ytd-banner-promo-renderer,
        [data-pagelet*="ad"],
        [data-testid="placementTracking"],
        .trc_related_container,[id*="taboola"],[class*="taboola"],
        .OUTBRAIN,[id*="outbrain"],[class*="outbrain"],
        [class*="push-notification"],[id*="push-subscribe"],
        [class*="sticky-ad"],[class*="fixed-ad"],[id*="sticky-ad"],
        iframe[src*="doubleclick"],iframe[src*="googlesyndication"],
        iframe[src*="adservice"],iframe[src*="moatads"],
        iframe[src*="taboola"],iframe[src*="outbrain"],
        iframe[src*="adnxs"],iframe[src*="amazon-adsystem"] {
            display:none!important;
            visibility:hidden!important;
            pointer-events:none!important;
            height:0!important;
            max-height:0!important;
            overflow:hidden!important;
        }
    """.trimIndent()

    private fun buildJs(escapedCss: String): String = """
        (function() {
            if (window.__novaCosmeticInstalled) return;
            window.__novaCosmeticInstalled = true;

            var style = document.createElement('style');
            style.id = 'nova-cosmetic';
            style.textContent = '$escapedCss';
            (document.head || document.documentElement).appendChild(style);

            var AD_PATTERNS = [
                'adsbygoogle','google-auto','taboola','outbrain',
                'moatads','adnxs','criteo','quantserve'
            ];

            function hideAdNode(el) {
                if (!el || el.nodeType !== 1) return;
                var cls = (el.className && typeof el.className === 'string') ? el.className.toLowerCase() : '';
                var id  = (el.id || '').toLowerCase();
                var src = ((el.tagName === 'IFRAME' || el.tagName === 'SCRIPT') && el.src)
                          ? el.src.toLowerCase() : '';
                for (var i = 0; i < AD_PATTERNS.length; i++) {
                    if (cls.indexOf(AD_PATTERNS[i]) !== -1 ||
                        id.indexOf(AD_PATTERNS[i])  !== -1 ||
                        src.indexOf(AD_PATTERNS[i]) !== -1) {
                        el.style.cssText = 'display:none!important;height:0!important;';
                        return;
                    }
                }
            }

            new MutationObserver(function(mutations) {
                for (var i = 0; i < mutations.length; i++) {
                    var nodes = mutations[i].addedNodes;
                    for (var j = 0; j < nodes.length; j++) { hideAdNode(nodes[j]); }
                }
            }).observe(document.documentElement, { childList: true, subtree: true });

            console.log('[Nova] Cosmetic filter installed');
        })();
    """.trimIndent()

    /** Inject cosmetic CSS + MutationObserver into the given WebView. */
    fun inject(webView: WebView) {
        val css = buildCss()
        val escapedCss = css
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "")
            .replace("\n", " ")
        webView.evaluateJavascript(buildJs(escapedCss), null)
    }
}
