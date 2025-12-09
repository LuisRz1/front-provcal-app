package com.sanna.provcalapp.ui.sanitary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentIncidentReportBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Incident Report Fragment - Detailed form for Inconforme result (POLÍTICAS 4)
 */
class IncidentReportFragment : Fragment() {

    private var _binding: FragmentIncidentReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentReportViewModel by viewModels()

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
        _binding = FragmentIncidentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupDropdowns()
        setupListeners()
        observeViewModel()

        viewModel.loadIncidentTypes(policyId)
        viewModel.loadCompanies()
    }

    private fun setupUI() {
        binding.tvPolicyType.text = policyName

        // Display current date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
        binding.tvDate.text = dateFormat.format(Date())

        // Inconforme is pre-selected, show incident fields initially
        binding.rbInconforme.isChecked = true
        toggleIncidentFields(true)
    }

    private fun setupDropdowns() {
        // Incident types dropdown will be populated when data loads
        binding.actvIncidentType.setOnClickListener {
            binding.actvIncidentType.showDropDown()
        }

        // Companies dropdown will be populated when data loads
        binding.actvCompany.setOnClickListener {
            binding.actvCompany.showDropDown()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            // Pop back stack until we reach Policy Management (POLÍTICAS 2)
            // This handles the case where we navigated from RevisionForm -> IncidentReport
            findNavController().popBackStack(
                com.sanna.provcalapp.R.id.policyManagementFragment,
                false
            )
        }

        // Show/hide incident fields based on result selection
        binding.radioGroupResult.setOnCheckedChangeListener { _, checkedId ->
            val isInconforme = checkedId == binding.rbInconforme.id
            toggleIncidentFields(isInconforme)
        }

        binding.btnSave.setOnClickListener {
            saveIncidentReport()
        }
    }

    private fun toggleIncidentFields(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE

        // Only hide incident type and company fields
        // Comment field stays visible for both Conforme and Inconforme
        binding.tvIncidentTypeLabel.visibility = visibility
        binding.tilIncidentType.visibility = visibility
        binding.tvCompanyLabel.visibility = visibility
        binding.tilCompany.visibility = visibility
    }

    private fun saveIncidentReport() {
        val isConforme = binding.rbConforme.isChecked
        val comment = binding.etIncidentComment.text?.toString() ?: ""

        // For Inconforme, validate incident-specific fields
        if (!isConforme) {
            val incidentTypeText = binding.actvIncidentType.text?.toString()
            val companyText = binding.actvCompany.text?.toString()

            if (incidentTypeText.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Por favor seleccione el tipo de incidencia", Toast.LENGTH_SHORT).show()
                return
            }

            if (companyText.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Por favor seleccione una empresa", Toast.LENGTH_SHORT).show()
                return
            }

            // Get IDs from selected items
            val incidentTypeId = viewModel.getIncidentTypeId(incidentTypeText)
            val companyId = viewModel.getCompanyId(companyText)

            viewModel.submitIncidentReport(
                policyId = policyId,
                isConforme = false,
                observation = comment,
                incidentTypeId = incidentTypeId,
                companyId = companyId
            )
        } else {
            // For Conforme, submit with comment but without incident details
            viewModel.submitIncidentReport(
                policyId = policyId,
                isConforme = true,
                observation = comment,
                incidentTypeId = null,
                companyId = null
            )
        }
    }

    private fun observeViewModel() {
        viewModel.incidentTypes.observe(viewLifecycleOwner) { types ->
            val typeNames = types.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
            )
            binding.actvIncidentType.setAdapter(adapter)
        }

        viewModel.companies.observe(viewLifecycleOwner) { companies ->
            val companyNames = companies.map { it.socialReason }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                companyNames
            )
            binding.actvCompany.setAdapter(adapter)
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Reporte guardado exitosamente", Toast.LENGTH_SHORT).show()
                // Go back to Policy Management
                findNavController().popBackStack(
                    com.sanna.provcalapp.R.id.policyManagementFragment,
                    false
                )
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
