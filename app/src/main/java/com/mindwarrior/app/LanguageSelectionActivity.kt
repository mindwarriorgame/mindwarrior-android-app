package com.mindwarrior.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityLanguageSelectionBinding

class LanguageSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.languageOptionEnglish.setOnClickListener { selectLanguage("en") }
        binding.languageOptionRussian.setOnClickListener { selectLanguage("ru") }
        binding.languageOptionFrench.setOnClickListener { selectLanguage("fr") }
        binding.languageOptionGerman.setOnClickListener { selectLanguage("de") }
        binding.languageOptionSpanish.setOnClickListener { selectLanguage("es") }
    }

    private fun selectLanguage(tag: String) {
        LanguageManager.setLanguage(this, tag)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
