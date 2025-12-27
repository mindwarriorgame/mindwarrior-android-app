package com.mindwarrior.app

import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityBoardWebviewBinding
import java.nio.charset.StandardCharsets

class BoardWebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBoardWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.boardWebview.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            loadDataWithBaseURL(
                BOARD_ASSET_PAGE_URL,
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

    companion object {
        private const val BOARD_ASSET_PAGE_URL =
            "file:///android_asset/miniapp-frontend/board.html?lang=en&env=prod&new_badge=s1&level=12&b1=c1_s1a_c2_c2_s1am_s2_t0_t0_c0_c0_c0&bp1=c1_49829_31--c0_0_100--t0_85829_20--s2_15_0&ts=1766402482"
        private const val BOARD_ASSET_PATH = "miniapp-frontend/board.html"
        private const val CONFETTI_CDN_URL =
            "https://cdnjs.cloudflare.com/ajax/libs/tsparticles-confetti/2.12.0/tsparticles.confetti.bundle.min.js"
        private const val CONFETTI_LOCAL_URL = "vendor/tsparticles-confetti.bundle.min.js"
        private const val TELEGRAM_CDN_URL = "https://telegram.org/js/telegram-web-app.js"
        private const val TELEGRAM_LOCAL_URL = "vendor/telegram-web-app.js"
    }

    private fun loadBoardHtml(): String {
        val rawHtml = assets.open(BOARD_ASSET_PATH).use { input ->
            input.bufferedReader(StandardCharsets.UTF_8).readText()
        }

        return rawHtml
            .replace(CONFETTI_CDN_URL, CONFETTI_LOCAL_URL)
            .replace(TELEGRAM_CDN_URL, TELEGRAM_LOCAL_URL)
    }
}
