package com.mindwarrior.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mindwarrior.app.databinding.ItemLogBinding

data class LogItem(
    val timeLabel: String,
    val message: String
)

class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    private val items = mutableListOf<LogItem>()

    fun prependLogs(newLogs: List<LogItem>) {
        if (newLogs.isEmpty()) return
        items.addAll(0, newLogs)
        notifyItemRangeInserted(0, newLogs.size)
    }

    fun trimTo(maxItems: Int) {
        if (items.size <= maxItems) return
        val removeCount = items.size - maxItems
        val startIndex = maxItems
        repeat(removeCount) {
            items.removeAt(items.size - 1)
        }
        notifyItemRangeRemoved(startIndex, removeCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class LogViewHolder(private val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LogItem) {
            binding.logTime.text = item.timeLabel
            binding.logMessage.text = item.message
        }
    }
}
