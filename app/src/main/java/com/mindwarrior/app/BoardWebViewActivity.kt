package com.mindwarrior.app

import android.os.Bundle
import android.webkit.JavascriptInterface
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityBoardWebviewBinding

class BoardWebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBoardWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val baseUrlExtra = intent.getStringExtra(EXTRA_BASE_URL)
        val assetPathExtra = intent.getStringExtra(EXTRA_ASSET_PATH)

        val loader = AssetWebViewLoader(assets)
        loader.loadInto(
            binding.boardWebview,
            AssetWebViewLoader.Config(
                baseUrl = (baseUrlExtra ?: BOARD_ASSET_PAGE_URL) +
                    "&ts=" + (java.util.Date().time / 1000),
                assetPath = assetPathExtra ?: BOARD_ASSET_PATH,
                replacements = AssetWebViewLoader.defaultReplacements(),
                javascriptInterfaceName = JS_INTERFACE_NAME,
                javascriptInterface = BoardWebViewBridge()
            )
        )
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
        const val EXTRA_BASE_URL = "extra_base_url"
        const val EXTRA_ASSET_PATH = "extra_asset_path"
        private const val JS_INTERFACE_NAME = "MindWarrior"
        private const val BOARD_ASSET_PAGE_URL =
            "file:///android_asset/miniapp-frontend/board.html?lang=en&env=prod&new_badge=s1&level=12&b1=c1_s1a_c2_c2_s1am_s2_t0_t0_c0_c0_c0&bp1=c1_49829_31--c0_0_100--t0_85829_20--s2_15_0"
        private const val BOARD_ASSET_PATH = "miniapp-frontend/board.html"
    }
}
