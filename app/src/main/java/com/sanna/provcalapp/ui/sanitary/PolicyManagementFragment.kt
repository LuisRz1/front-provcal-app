package com.sanna.provcalapp.ui.sanitary

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentPolicyManagementBinding
import com.sanna.provcalapp.databinding.DialogConfirmationBinding

/**
 * Policy Management Fragment - Shows history and scheduling
 * POLÍTICAS 2 screen
 */
class PolicyManagementFragment : Fragment() {

    private var _binding: FragmentPolicyManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PolicyManagementViewModel by viewModels()
    private lateinit var historyAdapter: RevisionHistoryAdapter

    private var policyId: String = ""
    private var policyName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        policyId = arguments?.getString("policyId") ?: ""
        policyName = arguments?.getString("policyName") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolicyManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupFilterChips()
        setupListeners()
        observeViewModel()

        // Load data
        viewModel.loadPolicyData(policyId)
    }

    private fun setupUI() {
        binding.tvPolicyType.text = policyName
    }

    private fun setupRecyclerView() {
        historyAdapter = RevisionHistoryAdapter { item ->
            // Show observation dialog
            showObservationDialog(item)
        }
        binding.rvHistory.adapter = historyAdapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val filterMonths = when (checkedIds.first()) {
                R.id.chipAll -> null
                R.id.chip6Months -> 6
                R.id.chip1Year -> 12
                R.id.chip2Years -> 24
                else -> null
            }
            viewModel.loadHistory(filterMonths)
        }
    }

    private fun setupListeners() {
        binding.tvLogout.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRevision.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.history.observe(viewLifecycleOwner) { history ->
            if (history.isEmpty()) {
                binding.rvHistory.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvHistory.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
                historyAdapter.submitList(history)
            }
        }

        viewModel.nextRevisionDate.observe(viewLifecycleOwner) { date ->
            if (date != null) {
                binding.tvNextRevisionDate.text = "Fecha: $date"
            } else {
                binding.tvNextRevisionDate.text = "No programada"
            }
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

    private fun showConfirmationDialog() {
        val dialog = Dialog(requireContext())
        val dialogBinding = DialogConfirmationBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Check if today is before the scheduled date
        val scheduledDate = viewModel.nextRevisionDate.value ?: "fecha desconocida"
        val isEarly = viewModel.isBeforeScheduledDate()

        if (isEarly) {
            dialogBinding.tvMessage.text = "programada para el $scheduledDate, pero puede realizarla ahora si lo desea"
        } else {
            dialogBinding.tvMessage.text = "para el $scheduledDate"
        }

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnContinue.setOnClickListener {
            dialog.dismiss()
            // Navigate to revision form
            navigateToRevisionForm()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun navigateToRevisionForm() {
        val bundle = Bundle().apply {
            putString("policyId", policyId)
            putString("policyName", policyName)
        }
        findNavController().navigate(
            R.id.action_policyManagement_to_revisionForm,
            bundle
        )
    }

    private fun showObservationDialog(item: com.sanna.provcalapp.data.models.RevisionHistoryItem) {
        val dialog = Dialog(requireContext())
        val dialogBinding = com.sanna.provcalapp.databinding.DialogObservationBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDate.text = item.date
        dialogBinding.tvIncidentType.text = item.incidentType ?: "N/A"
        dialogBinding.tvObservation.text = item.observation ?: "Sin observaciones"

        if (item.company != null) {
            dialogBinding.tvCompanyName.text = item.company.socialReason
            dialogBinding.tvCompanyPhone.text = item.company.phone ?: "N/A"
        } else {
            dialogBinding.tvCompanyName.text = "N/A"
            dialogBinding.tvCompanyPhone.text = "N/A"
        }

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // Set dialog width to 90% of screen width to match design
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
