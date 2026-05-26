package com.example.teainfoapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.teainfoapp.data.LogEntity
import com.example.teainfoapp.databinding.LogItemBinding
import java.text.SimpleDateFormat
import java.util.Locale

class LogAdapter(
    private val logs: MutableList<LogEntity>,
    val onDelete: (LogEntity, Int) -> Unit,
    private val onLongClick: (LogEntity) -> Unit
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    inner class LogViewHolder(val binding: LogItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = LogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.binding.apply {
            logTeaType.text = log.teaType
            logTimestamp.text = dateFormat.format(log.timestamp)
            logConfirmed.text = if (log.userConfirmed) "✓" else ""
            
            root.setOnLongClickListener {
                onLongClick(log)
                true
            }
        }
    }

    override fun getItemCount() = logs.size

    fun removeAt(position: Int): LogEntity {
        val log = logs.removeAt(position)
        notifyItemRemoved(position)
        return log
    }

    fun restoreAt(position: Int, log: LogEntity) {
        logs.add(position, log)
        notifyItemInserted(position)
    }
}
