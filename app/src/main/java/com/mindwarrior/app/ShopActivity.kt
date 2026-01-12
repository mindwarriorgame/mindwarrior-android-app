package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mindwarrior.app.badges.BadgesManager
import com.mindwarrior.app.databinding.ActivityShopBinding
import com.mindwarrior.app.engine.GameManager

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
        val badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
        val hasGrumpyCat = badgesManager.countActiveGrumpyCatsOnBoard() > 0
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
            user.diamonds,
            hasGrumpyCat
        )
        updateBuyState(
            binding.shopItemExpelGrumpyBuy,
            binding.shopItemExpelGrumpyPrice,
            R.string.shop_item_expel_grumpy_price,
            basePrice,
            user.diamonds,
            !hasGrumpyCat,
            R.string.shop_no_grumpy_to_expel
        )
        updateBuyState(
            binding.shopItemPreventAttackBuy,
            binding.shopItemPreventAttackPrice,
            R.string.shop_item_prevent_attack_price,
            repellerPrice,
            user.diamonds
        )

        binding.shopItemExpelGrumpyBuy.setOnClickListener {
            val currentUser = UserStorage.getUser(this)
            val currentBasePrice = SHOP_BASE_PRICES.getOrElse(currentUser.difficulty.ordinal) {
                SHOP_BASE_PRICES.last()
            }
            val currentBadgesManager =
                BadgesManager(currentUser.difficulty.ordinal, currentUser.badgesSerialized)
            val currentHasGrumpyCat = currentBadgesManager.countActiveGrumpyCatsOnBoard() > 0
            val canBuy = currentHasGrumpyCat && currentUser.diamonds >= currentBasePrice
            if (!canBuy) {
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.shop_confirm_title)
                .setMessage(getString(R.string.shop_confirm_expel_grumpy, currentBasePrice))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val updated = GameManager.onShooGrumpyCat(
                        currentUser,
                        getString(R.string.log_grumpy_removed)
                    )
                    if (updated == currentUser) {
                        return@setPositiveButton
                    }
                    val charged = updated.copy(
                        diamonds = (currentUser.diamonds - currentBasePrice).coerceAtLeast(0),
                        diamondsSpent = currentUser.diamondsSpent + currentBasePrice
                    )
                    UserStorage.upsertUser(this, charged)
                    finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

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
        diamonds: Int,
        blocked: Boolean = false,
        blockedMessageResId: Int = R.string.shop_grumpy_blocking
    ) {
        val needed = (price - diamonds).coerceAtLeast(0)
        val canBuy = needed == 0 && !blocked
        button.isEnabled = canBuy
        button.alpha = if (canBuy) 1f else 0.5f
        val priceText = getString(priceResId, price)
        priceView.text = when {
            canBuy -> priceText
            blocked -> priceText + " · " + getString(blockedMessageResId)
            else -> priceText + " · " + getString(R.string.shop_insufficient_diamonds, needed)
        }
        val colorRes = if (canBuy) R.color.menu_text else R.color.legend_red
        priceView.setTextColor(ContextCompat.getColor(this, colorRes))
    }
}
