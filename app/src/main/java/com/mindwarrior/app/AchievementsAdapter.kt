package com.mindwarrior.app

import android.animation.ValueAnimator
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.mindwarrior.app.databinding.ItemAchievementBinding

data class AchievementCell(
    val type: CellType,
    val imageRes: Int,
    val startPercent: Int = 0,
    val progressPercent: Int = 0,
    var hasAnimated: Boolean = false
)

enum class CellType {
    EMPTY,
    LOCKED,
    IN_PROGRESS,
    ACTIVE
}

class AchievementsAdapter(
    private val items: List<AchievementCell>
) : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AchievementViewHolder(private val binding: ItemAchievementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cell: AchievementCell) {
            binding.achievementImage.setImageResource(cell.imageRes)
            binding.achievementDim.visibility = View.GONE
            binding.lockIcon.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.achievementImage.colorFilter = null
            binding.achievementImage.alpha = 1f
            binding.lockIcon.colorFilter = null

            when (cell.type) {
                CellType.EMPTY -> {
                    binding.achievementImage.alpha = 0.35f
                }
                CellType.LOCKED -> {
                    applyGrayscale()
                    binding.achievementDim.visibility = View.VISIBLE
                    binding.lockIcon.visibility = View.VISIBLE
                    binding.lockIcon.setColorFilter(0xFFE6F0FF.toInt())
                }
                CellType.IN_PROGRESS -> {
                    applyGrayscale()
                    binding.achievementImage.alpha = 0.7f
                    binding.progressBar.visibility = View.VISIBLE
                    applyProgress(cell.startPercent)
                    if (!cell.hasAnimated) {
                        animateProgress(cell)
                    } else {
                        applyProgress(cell.progressPercent)
                    }
                }
                CellType.ACTIVE -> {
                    binding.achievementImage.alpha = 1f
                }
            }

        }

        private fun applyGrayscale() {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            binding.achievementImage.colorFilter = ColorMatrixColorFilter(matrix)
        }

        private fun applyProgress(progressPercent: Int) {
            val clamped = progressPercent.coerceIn(0, 100)
            val fillWeight = clamped / 100f
            val restWeight = 1f - fillWeight
            setWeight(binding.progressFill, fillWeight)
            setWeight(binding.progressRest, restWeight)
            val color = when {
                clamped < 33 -> 0xFFE24949.toInt()
                clamped < 66 -> 0xFFE3B642.toInt()
                else -> 0xFF39D37A.toInt()
            }
            binding.progressFill.setBackgroundColor(color)
        }

        private fun setWeight(view: View, weight: Float) {
            val params = view.layoutParams as LinearLayout.LayoutParams
            params.weight = weight.coerceAtLeast(0f)
            view.layoutParams = params
            view.visibility = if (weight <= 0f) View.GONE else View.VISIBLE
        }

        private fun animateProgress(cell: AchievementCell) {
            cell.hasAnimated = true
            val animator = ValueAnimator.ofInt(cell.startPercent, cell.progressPercent)
            animator.duration = 900L
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                applyProgress(value)
            }
            animator.start()
        }

    }
}
