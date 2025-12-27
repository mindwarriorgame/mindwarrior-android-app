package com.mindwarrior.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityMainBinding
import com.mindwarrior.app.viewmodel.HelloViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: HelloViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.message.observe(this) { message ->
            binding.helloText.text = message
        }
    }
}
