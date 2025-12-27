package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mindwarrior.app.databinding.ActivityAchievementsBinding
import kotlin.math.ceil
import kotlin.random.Random

class AchievementsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAchievementsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val spanCount = GRID_SPAN
        val totalCells = 10

        val adapter = AchievementsAdapter(buildRandomBoard(totalCells))
        binding.achievementsGrid.layoutManager = GridLayoutManager(this, spanCount)
        binding.achievementsGrid.adapter = adapter
        binding.achievementsGrid.addItemDecoration(
            BoardSpacingDecoration(
                spanCount = spanCount,
                spacingPx = resources.getDimensionPixelSize(R.dimen.board_cell_spacing)
            )
        )
    }

    private fun buildRandomBoard(cellCount: Int): List<AchievementCell> {
        val activeImages = listOf(
            R.drawable.badge_s0,
            R.drawable.badge_s1,
            R.drawable.badge_s2,
            R.drawable.badge_c0,
            R.drawable.badge_c1,
            R.drawable.badge_c2,
            R.drawable.badge_f,
            R.drawable.badge_t
        )
        return List(cellCount) {
            val roll = Random.nextInt(100)
            when {
                roll < 20 -> AchievementCell(CellType.EMPTY, R.drawable.badge_empty)
                roll < 45 -> AchievementCell(
                    CellType.LOCKED,
                    activeImages.random()
                )
                roll < 70 -> {
                    val start = Random.nextInt(10, 41)
                    val bump = Random.nextInt(5, 31)
                    val target = (start + bump).coerceAtMost(100)
                    AchievementCell(
                        CellType.IN_PROGRESS,
                        activeImages.random(),
                        startPercent = start,
                        progressPercent = target
                    )
                }
                else -> AchievementCell(CellType.ACTIVE, activeImages.random())
            }
        }
    }

    companion object {
        private const val GRID_SPAN = 4
    }

    private class BoardSpacingDecoration(
        private val spanCount: Int,
        private val spacingPx: Int
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: android.view.View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            val itemCount = state.itemCount
            val column = position % spanCount
            val row = position / spanCount
            val totalRows = ceil(itemCount / spanCount.toDouble()).toInt()
            val isLastRow = row == totalRows - 1
            val itemsInLastRow = if (itemCount % spanCount == 0) spanCount else itemCount % spanCount

            val half = spacingPx / 2
            outRect.left = if (column == 0) 0 else half
            outRect.right = if (column == spanCount - 1) 0 else half
            outRect.top = if (row == 0) 0 else spacingPx
            outRect.bottom = 0

            if (isLastRow && itemsInLastRow != spanCount) {
                val parentWidth = parent.width - parent.paddingStart - parent.paddingEnd
                val cellWidth =
                    (parentWidth - (spanCount - 1) * spacingPx) / spanCount
                val usedWidth =
                    itemsInLastRow * cellWidth + (itemsInLastRow - 1) * spacingPx
                val leftOffset = ((parentWidth - usedWidth) / 2f).toInt()
                if (column == 0) {
                    outRect.left += leftOffset
                }
            }
        }
    }
}
