package com.sanna.provcalapp.ui.sanitary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sanna.provcalapp.R
import com.sanna.provcalapp.data.models.RevisionHistoryItem
import com.sanna.provcalapp.databinding.ItemRevisionHistoryBinding

/**
 * Adapter for displaying revision history items with month badges
 */
class RevisionHistoryAdapter(
    private val onItemClick: (RevisionHistoryItem) -> Unit
) : RecyclerView.Adapter<RevisionHistoryAdapter.HistoryViewHolder>() {

    private var items: List<RevisionHistoryItem> = emptyList()

    fun submitList(newItems: List<RevisionHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemRevisionHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HistoryViewHolder(
        private val binding: ItemRevisionHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
        }

        fun bind(item: RevisionHistoryItem) {
            val context = binding.root.context

            // Month badge
            binding.tvMonthBadge.text = item.monthLabel
            val badgeBackground = if (item.isConforme) {
                R.drawable.bg_month_badge_green
            } else {
                R.drawable.bg_month_badge_red
            }
            binding.tvMonthBadge.setBackgroundResource(badgeBackground)

            // Date
            binding.tvDate.text = item.date

            // Result
            val resultText = if (item.isConforme) {
                context.getString(R.string.conforme)
            } else {
                context.getString(R.string.inconforme)
            }
            binding.tvResult.text = resultText

            // Status icon - checkmark for Conforme, document for Inconforme
            binding.ivStatus.visibility = android.view.View.VISIBLE
            if (item.isConforme) {
                binding.ivStatus.setImageResource(R.drawable.ic_check_circle)
                binding.ivStatus.setColorFilter(
                    ContextCompat.getColor(context, R.color.status_conforme)
                )
            } else {
                binding.ivStatus.setImageResource(R.drawable.ic_document)
                binding.ivStatus.setColorFilter(
                    ContextCompat.getColor(context, R.color.status_inconforme)
                )
            }
        }
    }
}
