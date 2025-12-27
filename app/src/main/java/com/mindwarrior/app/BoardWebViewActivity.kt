package com.mindwarrior.app

import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityBoardWebviewBinding

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
            loadUrl(BOARD_URL)
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
        private const val BOARD_URL =
            "https://mindwarrior-dev.netlify.app/miniapp-frontend/board.html?lang=en&env=prod&level=12&b1=c1a_s1a_c2_c2_s1a_s2a_t0a_t0_c0a_c0_c0&bp1=c2_4059_95--c0_7_65--t0_53127_50&ts=1766741702#tgWebAppData=query_id%3DAAFiz_xLAgAAAGLP_Ev0xk85%26user%3D%257B%2522id%2522%253A5569826658%252C%2522first_name%2522%253A%2522Boris%2522%252C%2522last_name%2522%253A%2522Gvozdev%2522%252C%2522username%2522%253A%2522BorisGdev%2522%252C%2522language_code%2522%253A%2522en%2522%252C%2522allows_write_to_pm%2522%253Atrue%252C%2522photo_url%2522%253A%2522https%253A%255C%252F%255C%252Ft.me%255C%252Fi%255C%252Fuserpic%255C%252F320%255C%252F4dDWLY3k7cbdeff6E4ebeXELHVM44gAeNjkXm8z4Esan0GDslaXpqNaZtyLTWRQG.svg%2522%257D%26auth_date%3D1766827098%26signature%3Digq8oCEFQqh2AEfOHP1UQDfgo5LRHixjOo6AjJASx-4KqPthF23SP2gsp_bdX1K9qXL6wzJXvBoKE1ccPiJ-AQ%26hash%3D82c58693b958bd62cb002ab0846dfbdb22144ea831075361306ebec59c9f5254&tgWebAppVersion=9.1&tgWebAppPlatform=web&tgWebAppThemeParams=%7B%22bg_color%22%3A%22%23212121%22%2C%22button_color%22%3A%22%238774e1%22%2C%22button_text_color%22%3A%22%23ffffff%22%2C%22hint_color%22%3A%22%23aaaaaa%22%2C%22link_color%22%3A%22%238774e1%22%2C%22secondary_bg_color%22%3A%22%23181818%22%2C%22text_color%22%3A%22%23ffffff%22%2C%22header_bg_color%22%3A%22%23212121%22%2C%22accent_text_color%22%3A%22%238774e1%22%2C%22section_bg_color%22%3A%22%23212121%22%2C%22section_header_text_color%22%3A%22%238774e1%22%2C%22subtitle_text_color%22%3A%22%23aaaaaa%22%2C%22destructive_text_color%22%3A%22%23ff595a%22%7D"
    }
}
