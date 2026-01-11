package com.mindwarrior.app

import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
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
        val config = AssetWebViewLoader.Config(
            baseUrl = (baseUrlExtra ?: DEFAULT_ASSET_PAGE_URL) +
                "&ts=" + (NowProvider.nowMillis() / 1000),
            assetPath = assetPathExtra ?: DEFAULT_ASSET_PATH,
            replacements = AssetWebViewLoader.defaultReplacements(),
            injectedScript = buildLocalStorageRestoreScript(),
            javascriptInterfaceName = JS_INTERFACE_NAME,
            javascriptInterface = WebViewBridge(isReviewMode, isFormulaMode)
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

    private fun saveLocalStorageAndFinish(isReviewMode: Boolean, isFormulaMode: Boolean) {
        binding.webview.evaluateJavascript(LOCAL_STORAGE_SNAPSHOT_JS) { result ->
            val user = UserStorage.getUser(this)
            var updated = user
            if (!result.isNullOrBlank() && result != "null" && result != "undefined") {
                updated = GameManager.onLocalStorageUpdated(
                    updated,
                    Optional.of(result),
                    isFormulaMode,
                    getString(R.string.log_new_badge),
                    getString(R.string.log_game_started),
                    getString(R.string.log_formula_updated),
                    getString(R.string.log_grumpy_blocking)
                )
            }
            if (isReviewMode) {
                val totalMinutes =
                    DifficultyHelper.getReviewFrequencyMillis(updated.difficulty) / 60_000L
                val hours = (totalMinutes / 60).toInt()
                val minutes = (totalMinutes % 60).toInt()
                val reviewMessage = getString(R.string.log_review_completed, hours, minutes)
                val rewardMessage = getString(R.string.log_review_reward)
                val noRewardMessage = getString(R.string.log_review_no_reward)
                updated = GameManager.onReviewCompleted(
                    updated,
                    reviewMessage,
                    rewardMessage,
                    noRewardMessage,
                    getString(R.string.log_game_resumed),
                    getString(R.string.log_new_badge),
                    getString(R.string.log_grumpy_removed),
                    getString(R.string.log_grumpy_remaining),
                    getString(R.string.log_achievements_unblocked),
                    getString(R.string.log_grumpy_expelling)
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
