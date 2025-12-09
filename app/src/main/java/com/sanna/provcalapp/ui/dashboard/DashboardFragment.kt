package com.sanna.provcalapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentDashboardBinding
import com.sanna.provcalapp.data.local.TokenManager

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DashboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Un solo adapter con el onClick centralizado
        adapter = DashboardAdapter { option ->
            when (option.key) {
                "menu_mes" -> findNavController()
                    .navigate(R.id.action_navigation_dashboard_to_menuFragment)
                "sanitary_control" -> findNavController()
                    .navigate(R.id.action_navigation_dashboard_to_sanitaryControlMenu)
                else -> {
                    // Placeholder: luego navegas a los demás destinos si quieres
                    Toast.makeText(requireContext(), getString(option.titleRes), Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvOptions.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvOptions.setHasFixedSize(true)
        binding.rvOptions.adapter = adapter

        // 2) Opciones base para todos
        val options = mutableListOf(
            DashboardOption("turnos", R.string.turnos_realizados, R.drawable.ic_person_placeholder),
            DashboardOption("cambio_turno", R.string.solicitud_cambio_turno, R.drawable.ic_person_placeholder),
            DashboardOption("vacaciones", R.string.solicitar_vacaciones, R.drawable.ic_person_placeholder)
        )

        // 3) Rol del usuario (el backend lo retorna en inglés)
        val roleRaw = TokenManager(requireContext()).getRole()
        val role = roleRaw?.trim()?.uppercase()
        val isNutritionist = role == "NUTRITIONIST" || role == "NUTRICIONISTA"

        if (isNutritionist) {
            options += DashboardOption("menu_mes", R.string.menu_mes, R.drawable.ic_person_placeholder)
            options += DashboardOption("sanitary_control", R.string.sanitary_control, R.drawable.ic_pest_control)
        }

        adapter.submit(options)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
