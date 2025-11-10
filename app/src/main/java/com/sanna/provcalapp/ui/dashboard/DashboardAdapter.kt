package com.sanna.provcalapp.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sanna.provcalapp.databinding.ItemDashboardOptionBinding

class DashboardAdapter(
    private val onClick: (DashboardOption) -> Unit
) : RecyclerView.Adapter<DashboardAdapter.VH>() {

    private val items = mutableListOf<DashboardOption>()

    fun submit(list: List<DashboardOption>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(private val binding: ItemDashboardOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DashboardOption) {
            binding.tvTitle.setText(item.titleRes)
            binding.ivIcon.setImageResource(item.iconRes)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemDashboardOptionBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size
}
