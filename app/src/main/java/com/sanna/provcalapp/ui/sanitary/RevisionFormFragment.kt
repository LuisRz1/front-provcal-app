package com.sanna.provcalapp.ui.sanitary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sanna.provcalapp.databinding.FragmentRevisionFormBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Revision Form Fragment - Simple form for Conforme result (POLÍTICAS 3)
 */
class RevisionFormFragment : Fragment() {

    private var _binding: FragmentRevisionFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RevisionFormViewModel by viewModels()

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
        _binding = FragmentRevisionFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        binding.tvPolicyType.text = policyName

        // Display current date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
        binding.tvDate.text = dateFormat.format(Date())
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            // Navigate back to Policy Management (POLÍTICAS 2)
            findNavController().popBackStack()
        }

        // When Inconforme is selected, navigate to incident report form
        binding.radioGroupResult.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbInconforme.id -> {
                    // Navigate to incident report form for detailed info
                    navigateToIncidentReport()
                }
                binding.rbConforme.id -> {
                    // Stay on simple form for Conforme
                }
            }
        }

        binding.btnSave.setOnClickListener {
            saveRevision()
        }
    }

    private fun saveRevision() {
        val isConforme = binding.rbConforme.isChecked
        val comment = binding.etComment.text?.toString() ?: ""

        if (!isConforme && !binding.rbInconforme.isChecked) {
            Toast.makeText(requireContext(), "Por favor seleccione un resultado", Toast.LENGTH_SHORT).show()
            return
        }

        // Only allow saving Conforme here; Inconforme goes to incident report
        if (!isConforme) {
            navigateToIncidentReport()
            return
        }

        viewModel.submitRevision(policyId, isConforme, comment, null, null)
    }

    private fun navigateToIncidentReport() {
        val bundle = Bundle().apply {
            putString("policyId", policyId)
            putString("policyName", policyName)
        }
        findNavController().navigate(
            com.sanna.provcalapp.R.id.action_revisionForm_to_incidentReport,
            bundle
        )
    }

    private fun observeViewModel() {
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Revisión guardada exitosamente", Toast.LENGTH_SHORT).show()
                // Go back to Policy Management
                findNavController().popBackStack()
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
