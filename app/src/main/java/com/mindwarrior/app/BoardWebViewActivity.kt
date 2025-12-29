package com.mindwarrior.app

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityBoardWebviewBinding
import java.nio.charset.StandardCharsets
import java.time.Instant

class BoardWebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBoardWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.boardWebview.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(BoardWebViewBridge(), "MindWarrior")
            webViewClient = WebViewClient()
            loadDataWithBaseURL(
                BOARD_ASSET_PAGE_URL + "&ts=" + (java.util.Date().time / 1000),
                loadBoardHtml(),
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    override fun onBackPressed() {
        if (binding.boardWebview.canGoBack()) {
            binding.boardWebview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private inner class BoardWebViewBridge {
        @JavascriptInterface
        fun close() {
            runOnUiThread { finish() }
        }
    }

    companion object {
        private const val BOARD_ASSET_PAGE_URL =
            "file:///android_asset/miniapp-frontend/board.html?lang=en&env=prod&new_badge=s1&level=12&b1=c1_s1a_c2_c2_s1am_s2_t0_t0_c0_c0_c0&bp1=c1_49829_31--c0_0_100--t0_85829_20--s2_15_0"
        private const val BOARD_ASSET_PATH = "miniapp-frontend/board.html"
        private const val CONFETTI_CDN_URL =
            "https://cdnjs.cloudflare.com/ajax/libs/tsparticles-confetti/2.12.0/tsparticles.confetti.bundle.min.js"
        private const val CONFETTI_LOCAL_URL = "vendor/tsparticles-confetti.bundle.min.js"
    }

    private fun loadBoardHtml(): String {
        val rawHtml = assets.open(BOARD_ASSET_PATH).use { input ->
            input.bufferedReader(StandardCharsets.UTF_8).readText()
        }

        return rawHtml
            .replace(CONFETTI_CDN_URL, CONFETTI_LOCAL_URL)
            .replace("<script src=\"https://telegram.org/js/telegram-web-app.js?59\"></script>", "")
    }
}
