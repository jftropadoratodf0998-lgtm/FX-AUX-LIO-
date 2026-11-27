package com.auxiliofx.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString + " AuxilioFXApp/1.0"
        }

        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Bridge(), "AndroidFx")
        web.loadUrl("https://auxilio-fx-pro.base44.app")

        ShizukuBridge.init(this)
    }

    override fun onBackPressed() {
        if (this::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    inner class Bridge {

        @JavascriptInterface
        fun platform(): String = "android"

        @JavascriptInterface
        fun overlay(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this@MainActivity)
            ) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + packageName)
                    )
                )
                return false
            }
            ContextCompat.startForegroundService(
                this@MainActivity,
                Intent(this@MainActivity, OverlayService::class.java)
            )
            return true
        }

        @JavascriptInterface
        fun stopOverlay() {
            stopService(Intent(this@MainActivity, OverlayService::class.java))
        }

        @JavascriptInterface
        fun shizukuStatus(): String = ShizukuBridge.status()

        @JavascriptInterface
        fun requestShizuku(): Boolean = ShizukuBridge.request(this@MainActivity)

        @JavascriptInterface
        fun runCommand(command: String): String = ShizukuBridge.run(command)

        @JavascriptInterface
        fun boostSensi(): String = ShizukuBridge.run(FxCommands.boostSensi())

        @JavascriptInterface
        fun optimize(): String = ShizukuBridge.run(FxCommands.performance())

        @JavascriptInterface
        fun openGame(): Boolean {
            val launcher = packageManager.getLaunchIntentForPackage(FxCommands.PACKAGE_FF)
                ?: packageManager.getLaunchIntentForPackage(FxCommands.PACKAGE_FF_MAX)
                ?: return false
            startActivity(launcher)
            return true
        }
    }
}
