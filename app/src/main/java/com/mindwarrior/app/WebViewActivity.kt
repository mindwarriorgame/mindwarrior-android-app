package com.mindwarrior.app

import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityWebviewBinding
import com.mindwarrior.app.engine.GameManager
import java.util.Optional

class WebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val baseUrlExtra = intent.getStringExtra(EXTRA_BASE_URL)
        val assetPathExtra = intent.getStringExtra(EXTRA_ASSET_PATH)

        val loader = AssetWebViewLoader(assets)
        loader.loadInto(
            binding.webview,
            AssetWebViewLoader.Config(
                baseUrl = (baseUrlExtra ?: DEFAULT_ASSET_PAGE_URL) +
                    "&ts=" + (java.util.Date().time / 1000),
                assetPath = assetPathExtra ?: DEFAULT_ASSET_PATH,
                replacements = AssetWebViewLoader.defaultReplacements(),
                injectedScript = buildLocalStorageRestoreScript(),
                javascriptInterfaceName = JS_INTERFACE_NAME,
                javascriptInterface = WebViewBridge()
            )
        )
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private inner class WebViewBridge {
        @JavascriptInterface
        fun close() {
            runOnUiThread { saveLocalStorageAndFinish() }
        }
    }

    private fun saveLocalStorageAndFinish() {
        binding.webview.evaluateJavascript(LOCAL_STORAGE_SNAPSHOT_JS) { result ->
            if (!result.isNullOrBlank() && result != "null" && result != "undefined") {
                val user = UserStorage.getUser(this)
                UserStorage.upsertUser(
                    this,
                    GameManager.onLocalStorageUpdated(user, Optional.of(result))
                )
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
