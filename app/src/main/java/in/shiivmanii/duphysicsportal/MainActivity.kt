package `in`.shiivmanii.duphysicsportal

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    companion object {
        private const val PORTAL_URL =
            "https://physics.du.ac.in/stdnep/slogin.php"

        // Desktop browser identity used on EVERY page of the portal.
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/131.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        window.statusBarColor = Color.rgb(39, 45, 55)
        window.navigationBarColor = Color.BLACK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // App branding header with safe top padding so it never overlaps the status bar or camera notch
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), getStatusBarHeight() + dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(39, 45, 55))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Dynamically adjust header & bottom insets for Android 15+ Edge-to-Edge screens
        root.setOnApplyWindowInsetsListener { _, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()).top
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetTop
            }

            val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }

            val safeTop = if (topInset > 0) topInset else getStatusBarHeight()
            header.setPadding(dp(12), safeTop + dp(8), dp(12), dp(10))
            root.setPadding(0, 0, 0, bottomInset)
            insets
        }

        val title = TextView(this).apply {
            text = "DU Physics Portal"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "By shiivmanii"
            setTextColor(Color.LTGRAY)
            textSize = 12f
            gravity = Gravity.CENTER
        }

        header.addView(title)
        header.addView(subtitle)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            setBackgroundColor(Color.WHITE)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true

                // ALWAYS request the desktop site on every page
                userAgentString = DESKTOP_USER_AGENT

                // ALWAYS enable desktop wide viewport and overview scaling
                useWideViewPort = true
                loadWithOverviewMode = true

                // Allow pinch-to-zoom on all pages
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false

                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    view.settings.userAgentString = DESKTOP_USER_AGENT
                    return false
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String
                ): Boolean {
                    view.settings.userAgentString = DESKTOP_USER_AGENT
                    return false
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    CookieManager.getInstance().flush()
                    fitDesktopPageToScreen(view, url)
                }
            }
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        root.addView(header)
        root.addView(webView)
        setContentView(root)

        val restoredState = if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            null
        }

        if (restoredState == null) {
            webView.loadUrl(PORTAL_URL)
        }
    }

    /**
     * Fallback helper to read the phone's status bar height in pixels.
     */
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            dp(28)
        }
    }

    /**
     * Keeps every page in Desktop Mode while fitting the desktop canvas
     * cleanly to the phone screen.
     */
    private fun fitDesktopPageToScreen(view: WebView, url: String?) {
        val lowerUrl = url?.lowercase() ?: ""
        val isLoginCardPage = lowerUrl.contains("slogin.php") ||
                lowerUrl.contains("login") ||
                lowerUrl.contains("register")

        val js = if (isLoginCardPage) {
            """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=500, user-scalable=yes';

                var style = document.getElementById('du_desktop_fix');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'du_desktop_fix';
                    document.head.appendChild(style);
                }
                style.innerHTML = `
                    html, body {
                        margin: 0 auto !important;
                        padding: 10px !important;
                        width: 100% !important;
                        box-sizing: border-box !important;
                        display: flex !important;
                        flex-direction: column !important;
                        align-items: center !important;
                    }
                `;
                window.scrollTo(0, 0);
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=1280, user-scalable=yes';

                var style = document.getElementById('du_desktop_fix');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'du_desktop_fix';
                    document.head.appendChild(style);
                }
                style.innerHTML = `
                    html, body {
                        margin: 0 auto !important;
                        padding: 0 !important;
                    }
                    th, td {
                        white-space: normal !important;
                        word-break: normal !important;
                        overflow-wrap: normal !important;
                    }
                `;
                window.scrollTo(0, 0);
            })();
            """.trimIndent()
        }

        view.evaluateJavascript(js, null)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) {
            webView.saveState(outState)
        }
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated by Android; retained for compatibility")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            CookieManager.getInstance().flush()
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}