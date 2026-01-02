package com.mindwarrior.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindwarrior.app.databinding.ItemLogBinding

data class LogItem(
    val id: Long,
    val timestampMillis: Long,
    val timeLabel: String,
    val message: String
)

class LogAdapter : ListAdapter<LogItem, LogAdapter.LogViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(private val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LogItem) {
            binding.logTime.text = item.timeLabel
            binding.logMessage.text = item.message
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<LogItem>() {
        override fun areItemsTheSame(oldItem: LogItem, newItem: LogItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LogItem, newItem: LogItem): Boolean {
            return oldItem == newItem
        }
    }
}
