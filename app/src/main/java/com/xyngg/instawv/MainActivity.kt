package com.xyngg.instawv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private var instaWebView: WebView? = null
    private var instaWebSettings: WebSettings? = null
    private var mapsCookieManager: CookieManager? = null

    private val allowedDomains = ArrayList<String>()
    private val allowedDomainsStart = ArrayList<String>()
    private val allowedDomainsEnd = ArrayList<String>()
    private val blockedURLs = ArrayList<String>()

    private val consentDateFormat: DateFormat = SimpleDateFormat("yyyyMMdd")
    private val TAG = "GMapsWV"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(R.layout.activity_main)

        var urlToLoad = "https://www.instagram.com"
        //try {
        //    val data = intent.data
        //    urlToLoad = data.toString()
        //    if (data.toString().startsWith("https://")) {
        //        urlToLoad = data.toString()
        //    }
        //} catch (e: Exception) {
        //    Log.d(TAG, "No or Invalid URL passed. Opening homepage instead.")
        //}

        //Create the WebView
        instaWebView = findViewById<View>(R.id.InstaWebView) as WebView

        //Set cookie options
        var mapsCookieManager = CookieManager.getInstance()
        mapsCookieManager.setAcceptCookie(true)
        mapsCookieManager.setAcceptThirdPartyCookies(instaWebView, false)

        //Delete anything from previous sessions
//        resetWebView(false)

        //Set the consent cookie to prevent unnecessary redirections
        setConsentCookie()

        //Restrict what gets loaded
        initURLs()
        instaWebView!!.webViewClient = object : WebViewClient() {
            var hostname = "localhost" /*127.0.0.1*/
            var port = 1080
            var proxy: Proxy = Proxy(
                Proxy.Type.SOCKS,
                InetSocketAddress(hostname, port)
            )
            private val okHttp: OkHttpClient = OkHttpClient.Builder().proxy(proxy).build()

            //Keep these in sync!
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                if (request.url.toString() == "about:blank") {
                    return null
                }
                if (!request.url.toString().startsWith("https://")) {
                    Log.d(
                        TAG,
                        "[shouldInterceptRequest][NON-HTTPS] Blocked access to " + request.url.toString()
                    )
                    return WebResourceResponse(
                        "text/javascript",
                        "UTF-8",
                        null
                    ) //Deny URLs that aren't HTTPS
                }
                var allowed = false
                for (url in allowedDomains) {
                    if (request.url.host == url) {
                        allowed = true
                    }
                }
                for (url in allowedDomainsStart) {
                    if (request.url.host!!.startsWith(url!!)) {
                        allowed = true
                    }
                }
                for (url in allowedDomainsEnd) {
                    if (request.url.host!!.endsWith(url!!)) {
                        allowed = true
                    }
                }
                if (!allowed) {
                    Log.d(
                        TAG,
                        "[shouldInterceptRequest][NOT ON ALLOWLIST] Blocked access to " + request.url.host
                    )
                    return WebResourceResponse(
                        "text/javascript",
                        "UTF-8",
                        null
                    ) //Deny URLs not on ALLOWLIST
                }
                for (url in blockedURLs) {
                    if (request.url.toString().contains(url!!)) {
                        if (request.url.toString().contains("/log204?")) {
                            Log.d(
                                TAG,
                                "[shouldInterceptRequest][ON DENYLIST] Blocked access to a log204 request"
                            )
                        } else {
                            Log.d(
                                TAG,
                                "[shouldInterceptRequest][ON DENYLIST] Blocked access to " + request.url.toString()
                            )
                        }
                        return WebResourceResponse(
                            "text/javascript",
                            "UTF-8",
                            null
                        ) //Deny URLs on DENYLIST
                    }
                }
                val okHttpRequest: Request = Request.Builder().url(request.url.toString()).build()
                try {
                    val response: Response = okHttp.newCall(okHttpRequest).execute()
                    return WebResourceResponse("", "", response.body!!.byteStream())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                return null
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                if (request.url.toString() == "about:blank") {
                    return false
                }
                if (request.url.toString().startsWith("tel:")) {
                    val dial = Intent(Intent.ACTION_DIAL, request.url)
                    startActivity(dial)
                    return true
                }
                if (!request.url.toString().startsWith("https://")) {
                    Log.d(
                        TAG,
                        "[shouldOverrideUrlLoading][NON-HTTPS] Blocked access to " + request.url.toString()
                    )
                    return true //Deny URLs that aren't HTTPS
                }
                var allowed = false
                for (url in allowedDomains) {
                    if (request.url.host == url) {
                        allowed = true
                    }
                }
                for (url in allowedDomainsStart) {
                    if (request.url.host!!.startsWith(url!!)) {
                        allowed = true
                    }
                }
                for (url in allowedDomainsEnd) {
                    if (request.url.host!!.endsWith(url!!)) {
                        allowed = true
                    }
                }
                if (!allowed) {
                    Log.d(
                        TAG,
                        "[shouldOverrideUrlLoading][NOT ON ALLOWLIST] Blocked access to " + request.url.host
                    )
                    return true //Deny URLs not on ALLOWLIST
                }
                for (url in blockedURLs) {
                    if (request.url.toString().contains(url!!)) {
                        Log.d(
                            TAG,
                            "[shouldOverrideUrlLoading][ON DENYLIST] Blocked access to " + request.url.toString()
                        )
                        return true //Deny URLs on DENYLIST
                    }
                }
                return false
            }
        }

        //Set more options
        var instaWebSettings = instaWebView!!.settings
        //Enable some WebView features
        instaWebSettings.javaScriptEnabled = true
        instaWebSettings.cacheMode = WebSettings.LOAD_DEFAULT
        instaWebSettings.setGeolocationEnabled(true)
        //Disable some WebView features
        instaWebSettings.allowContentAccess = false
        instaWebSettings.allowFileAccess = false
        instaWebSettings.builtInZoomControls = false
        instaWebSettings.databaseEnabled = false
        instaWebSettings.displayZoomControls = false
        instaWebSettings.domStorageEnabled = false
        instaWebSettings.saveFormData = false
        //Change the User-Agent
        instaWebSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 12; Unspecified Device) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.79 Mobile Safari/537.36")

        //Load Instagram
        instaWebView!!.loadUrl(urlToLoad)
    }

    override fun onDestroy() {
        super.onDestroy()
//        resetWebView(true)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (instaWebView!!.canGoBack() && instaWebView!!.url != "about:blank") {
                        instaWebView!!.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun resetWebView(exit: Boolean) {
        if (exit) {
            instaWebView!!.loadUrl("about:blank")
            instaWebView!!.removeAllViews()
            instaWebSettings!!.javaScriptEnabled = false
        }
        //instaWebView.clearCache(true);
        instaWebView!!.clearFormData()
        instaWebView!!.clearHistory()
        instaWebView!!.clearMatches()
        instaWebView!!.clearSslPreferences()
//        mapsCookieManager!!.removeSessionCookie()
//        mapsCookieManager!!.removeAllCookie()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        if (exit) {
            instaWebView!!.destroyDrawingCache()
            instaWebView!!.destroy()
            instaWebView = null
        }
    }

    private fun setConsentCookie() {
        val consentDate: String = consentDateFormat.format(System.currentTimeMillis())
        val random = Random()
        val random2digit = random.nextInt(2) + 15
        val random3digit = random.nextInt(999)
        val consentCookie = "YES+cb.$consentDate-$random2digit-p1.en+F+$random3digit"
//        mapsCookieManager!!.setCookie(".google.com", "CONSENT=$consentCookie;")
        //mapsCookieManager.setCookie(".google.com", "CONSENT=PENDING+" + random3digit + ";"); //alternative
//        mapsCookieManager!!.setCookie(".google.com", "ANID=OPT_OUT;")
    }

    private fun initURLs() {
        //Allowed Domains
        allowedDomains.add("instagram.com")
        allowedDomains.add("www.instagram.com")
        allowedDomains.add("static.cdninstagram.com")

        //Blocked Domains
        //blockedURLs.add("analytics.google.com");

        //Blocked URLs
        //blockedURLs.add("google.com/maps/preview/log204");
    }
}