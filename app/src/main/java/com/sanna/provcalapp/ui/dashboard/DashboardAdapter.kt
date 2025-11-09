package com.sanna.provcalapp.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sanna.provcalapp.R

class DashboardAdapter(
    private val items: List<DashboardOption>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<DashboardAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val card = view as MaterialCardView
        private val icon: ImageView = card.findViewById(R.id.ivIcon)
        private val title: TextView = card.findViewById(R.id.tvTitle)
        fun bind(item: DashboardOption) {
            icon.setImageResource(item.iconRes)
            title.text = item.title
            card.setOnClickListener { onClick(item.navAction) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_option, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size
}
