package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mindwarrior.app.databinding.ActivityShopBinding

class ShopActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShopBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = UserStorage.getUser(this)
        val basePrice = SHOP_BASE_PRICES.getOrElse(user.difficulty.ordinal) {
            SHOP_BASE_PRICES.last()
        }
        val repellerPrice = (basePrice * 1.5f).toInt()
        binding.shopAvailableDiamonds.text = getString(
            R.string.shop_available_diamonds,
            user.diamonds
        )
        binding.shopItemNextAchievementPrice.text = getString(
            R.string.shop_item_next_achievement_price,
            basePrice
        )
        binding.shopItemExpelGrumpyPrice.text = getString(
            R.string.shop_item_expel_grumpy_price,
            basePrice
        )
        binding.shopItemPreventAttackPrice.text = getString(
            R.string.shop_item_prevent_attack_price,
            repellerPrice
        )
        updateBuyState(
            binding.shopItemNextAchievementBuy,
            binding.shopItemNextAchievementPrice,
            R.string.shop_item_next_achievement_price,
            basePrice,
            user.diamonds
        )
        updateBuyState(
            binding.shopItemExpelGrumpyBuy,
            binding.shopItemExpelGrumpyPrice,
            R.string.shop_item_expel_grumpy_price,
            basePrice,
            user.diamonds
        )
        updateBuyState(
            binding.shopItemPreventAttackBuy,
            binding.shopItemPreventAttackPrice,
            R.string.shop_item_prevent_attack_price,
            repellerPrice,
            user.diamonds
        )

        binding.shopClose.setOnClickListener {
            finish()
        }
    }

    companion object {
        private val SHOP_BASE_PRICES = listOf(10, 20, 30, 40, 50, 60)
    }

    private fun updateBuyState(
        button: androidx.appcompat.widget.AppCompatButton,
        priceView: android.widget.TextView,
        priceResId: Int,
        price: Int,
        diamonds: Int
    ) {
        val needed = (price - diamonds).coerceAtLeast(0)
        val canBuy = needed == 0
        button.isEnabled = canBuy
        button.alpha = if (canBuy) 1f else 0.5f
        val priceText = getString(priceResId, price)
        priceView.text = if (canBuy) {
            priceText
        } else {
            priceText + " · " + getString(R.string.shop_insufficient_diamonds, needed)
        }
        val colorRes = if (canBuy) R.color.menu_text else R.color.legend_red
        priceView.setTextColor(ContextCompat.getColor(this, colorRes))
    }
}
