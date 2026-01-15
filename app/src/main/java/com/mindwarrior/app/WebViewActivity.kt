package com.mindwarrior.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityWebviewBinding
import com.mindwarrior.app.engine.DifficultyHelper
import com.mindwarrior.app.engine.GameManager
import com.mindwarrior.app.NowProvider
import java.util.Optional

class WebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val baseUrlExtra = intent.getStringExtra(EXTRA_BASE_URL)
        val assetPathExtra = intent.getStringExtra(EXTRA_ASSET_PATH)
        val isReviewMode = intent.getBooleanExtra(EXTRA_IS_REVIEW, false)
        val isFormulaMode = intent.getBooleanExtra(EXTRA_IS_FORMULA, false)

        val loader = AssetWebViewLoader(assets)
        val langCode = LanguageManager.getCurrentLanguageTag(this)
        val config = AssetWebViewLoader.Config(
            baseUrl = appendLangParam(baseUrlExtra ?: DEFAULT_ASSET_PAGE_URL, langCode) +
                "&ts=" + (NowProvider.nowMillis() / 1000),
            assetPath = assetPathExtra ?: DEFAULT_ASSET_PATH,
            replacements = AssetWebViewLoader.defaultReplacements(),
            injectedScript = buildLocalStorageRestoreScript(),
            javascriptInterfaceName = JS_INTERFACE_NAME,
            javascriptInterface = WebViewBridge(isReviewMode, isFormulaMode),
            webViewClient = ExternalLinkWebViewClient()
        )
        loader.configure(binding.webview, config)
        val restored = savedInstanceState?.let { binding.webview.restoreState(it) }
        if (restored == null) {
            loader.loadContent(binding.webview, config)
        }
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webview.saveState(outState)
    }

    private inner class WebViewBridge(
        private val isReviewMode: Boolean,
        private val isFormulaMode: Boolean
    ) {
        @JavascriptInterface
        fun close() {
            runOnUiThread { saveLocalStorageAndFinish(isReviewMode, isFormulaMode) }
        }
    }

    private inner class ExternalLinkWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url ?: return false
            return handleExternalUrl(url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return url?.let { handleExternalUrl(Uri.parse(it)) } ?: false
        }

        private fun handleExternalUrl(uri: Uri): Boolean {
            val scheme = uri.scheme?.lowercase()
            if (scheme == "file" || scheme == "about" || scheme == "data" || scheme == "javascript") {
                return false
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            return if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                true
            } else {
                false
            }
        }
    }

    private fun saveLocalStorageAndFinish(isReviewMode: Boolean, isFormulaMode: Boolean) {
        binding.webview.evaluateJavascript(LOCAL_STORAGE_SNAPSHOT_JS) { result ->
            val user = UserStorage.getUser(this)
            var updated = user
            if (!result.isNullOrBlank() && result != "null" && result != "undefined") {
                updated = GameManager.onLocalStorageUpdated(
                    updated,
                    Optional.of(result),
                    isFormulaMode
                )
            }
            if (isReviewMode) {
                val totalMinutes =
                    DifficultyHelper.getReviewFrequencyMillis(updated.difficulty) / 60_000L
                val hours = (totalMinutes / 60).toInt()
                val minutes = (totalMinutes % 60).toInt()
                updated = GameManager.onReviewCompleted(
                    updated,
                    hours,
                    minutes
                )
            }
            if (updated != user) {
                UserStorage.upsertUser(this, updated)
            }
            finish()
        }
    }

    private fun buildLocalStorageRestoreScript(): String? {
        val snapshot = UserStorage.getUser(this).localStorageSnapshot
        if (!snapshot.isPresent) return null
        val encoded = Base64.encodeToString(
            snapshot.get().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        return """
            <script>
            (function() {
              try {
                var decoded = atob("$encoded");
                var json;
                if (window.TextDecoder) {
                  var bytes = Uint8Array.from(decoded, function(c) { return c.charCodeAt(0); });
                  json = new TextDecoder("utf-8").decode(bytes);
                } else {
                  json = decodeURIComponent(escape(decoded));
                }
                var data = JSON.parse(json);
                if (data && typeof data === "object") {
                  localStorage.clear();
                  Object.keys(data).forEach(function(key) {
                    localStorage.setItem(key, data[key]);
                  });
                }
              } catch (e) {}
            })();
            </script>
        """.trimIndent()
    }

    private fun appendLangParam(url: String, langCode: String): String {
        if (langCode.isBlank()) {
            return url
        }
        var updated = url
        if (!updated.contains("lang=")) {
            updated = if (updated.contains("?")) {
                "$updated&lang=$langCode"
            } else {
                "$updated?lang=$langCode"
            }
        }
        if (!updated.contains("lang_code=")) {
            updated = if (updated.contains("?")) {
                "$updated&lang_code=$langCode"
            } else {
                "$updated?lang_code=$langCode"
            }
        }
        return updated
    }

    companion object {
        const val EXTRA_BASE_URL = "extra_base_url"
        const val EXTRA_ASSET_PATH = "extra_asset_path"
        const val EXTRA_IS_REVIEW = "extra_is_review"
        const val EXTRA_IS_FORMULA = "extra_is_formula"
        private const val JS_INTERFACE_NAME = "MindWarrior"
        private const val DEFAULT_ASSET_PAGE_URL =
            "file:///android_asset/miniapp-frontend/board.html?lang=en&env=prod&new_badge=s1&level=12&b1=c1_s1a_c2_c2_s1am_s2_t0_t0_c0_c0_c0&bp1=c1_49829_31--c0_0_100--t0_85829_20--s2_15_0"
        private const val DEFAULT_ASSET_PATH = "miniapp-frontend/board.html"
        private const val LOCAL_STORAGE_SNAPSHOT_JS =
            "(function(){try{var data={};" +
                "for(var i=0;i<localStorage.length;i++){" +
                "var k=localStorage.key(i);" +
                "data[k]=localStorage.getItem(k);" +
                "}" +
                "return data;}catch(e){return null;}})();"
    }
}
