package com.sanna.provcalapp.ui.sanitary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sanna.provcalapp.R
import com.sanna.provcalapp.data.models.HealthPolicy
import com.sanna.provcalapp.data.models.PolicyType
import com.sanna.provcalapp.databinding.ItemPolicyCardBinding

/**
 * Adapter for displaying health policy cards in a grid
 */
class PolicyMenuAdapter(
    private val onPolicyClick: (HealthPolicy) -> Unit
) : RecyclerView.Adapter<PolicyMenuAdapter.PolicyViewHolder>() {

    private var policies: List<HealthPolicy> = emptyList()

    fun submitList(newPolicies: List<HealthPolicy>) {
        policies = newPolicies
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PolicyViewHolder {
        val binding = ItemPolicyCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PolicyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PolicyViewHolder, position: Int) {
        holder.bind(policies[position])
    }

    override fun getItemCount(): Int = policies.size

    inner class PolicyViewHolder(
        private val binding: ItemPolicyCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPolicyClick(policies[position])
                }
            }
        }

        fun bind(policy: HealthPolicy) {
            binding.tvTitle.text = policy.name.uppercase()

            // Set icon based on policy type
            val iconRes = when (policy.type) {
                PolicyType.PEST_CONTROL -> R.drawable.ic_pest_control
                PolicyType.WASTE_MANAGEMENT -> R.drawable.ic_waste_management
                PolicyType.DINING_SANITATION -> R.drawable.ic_dining_sanitation
            }
            // Set same icon on both left and right sides
            binding.ivIconLeft.setImageResource(iconRes)
            binding.ivIconRight.setImageResource(iconRes)
        }
    }
}
