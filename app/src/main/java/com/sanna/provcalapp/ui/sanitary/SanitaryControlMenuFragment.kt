package com.sanna.provcalapp.ui.sanitary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentSanitaryControlMenuBinding

/**
 * Main menu fragment for Sanitary Control (Health Policy)
 * Displays 3 policy cards: Pest Control, Waste Management, Dining Sanitation
 */
class SanitaryControlMenuFragment : Fragment() {

    private var _binding: FragmentSanitaryControlMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SanitaryControlViewModel by viewModels()
    private lateinit var adapter: PolicyMenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSanitaryControlMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = PolicyMenuAdapter { policy ->
            // Navigate to policy management screen with policy ID
            val bundle = Bundle().apply {
                putString("policyId", policy.id)
                putString("policyName", policy.name)
            }
            findNavController().navigate(R.id.actionSanitaryControlMenuToPolicyManagement, bundle)
        }

        binding.rvPolicyCards.apply {
            layoutManager = GridLayoutManager(requireContext(), 1)
            this.adapter = this@SanitaryControlMenuFragment.adapter
        }
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvLogout.setOnClickListener {
            // Navigate to logout or show logout confirmation
            // For now, just navigate back
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.policies.observe(viewLifecycleOwner) { policies ->
            adapter.submitList(policies)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Show/hide loading indicator
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
