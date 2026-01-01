package com.mindwarrior.app

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.nio.charset.StandardCharsets

class AssetWebViewLoader(private val assets: AssetManager) {
    data class Config(
        val baseUrl: String,
        val assetPath: String,
        val replacements: List<Pair<String, String>> = emptyList(),
        val javascriptInterfaceName: String? = null,
        val javascriptInterface: Any? = null
    )

    @SuppressLint("SetJavaScriptEnabled")
    fun loadInto(webView: WebView, config: Config) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        if (config.javascriptInterface != null && config.javascriptInterfaceName != null) {
            webView.addJavascriptInterface(
                config.javascriptInterface,
                config.javascriptInterfaceName
            )
        }
        webView.webViewClient = WebViewClient()
        webView.loadDataWithBaseURL(
            config.baseUrl,
            loadHtml(config.assetPath, config.replacements),
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun loadHtml(assetPath: String, replacements: List<Pair<String, String>>): String {
        val rawHtml = assets.open(assetPath).use { input ->
            input.bufferedReader(StandardCharsets.UTF_8).readText()
        }
        return replacements.fold(rawHtml) { acc, (from, to) -> acc.replace(from, to) }
    }

    companion object {
        private const val CONFETTI_CDN_URL =
            "https://cdnjs.cloudflare.com/ajax/libs/tsparticles-confetti/2.12.0/tsparticles.confetti.bundle.min.js"
        private const val CONFETTI_LOCAL_URL = "vendor/tsparticles-confetti.bundle.min.js"
        private const val TELEGRAM_SCRIPT =
            "<script src=\"https://telegram.org/js/telegram-web-app.js?59\"></script>"

        fun defaultReplacements(): List<Pair<String, String>> {
            return listOf(
                CONFETTI_CDN_URL to CONFETTI_LOCAL_URL,
                TELEGRAM_SCRIPT to ""
            )
        }
    }
}
