package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityShopBinding

class ShopActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShopBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = UserStorage.getUser(this)
        binding.shopAvailableDiamonds.text = getString(
            R.string.shop_available_diamonds,
            user.diamonds
        )

        binding.shopClose.setOnClickListener {
            finish()
        }
    }
}
