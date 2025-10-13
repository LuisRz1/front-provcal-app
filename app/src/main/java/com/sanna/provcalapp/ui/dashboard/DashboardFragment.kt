package com.sanna.provcalapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val options = listOf(
            DashboardOption(
                title = getString(R.string.turnos_realizados),
                iconRes = R.drawable.ic_calendar_24,
                navAction = R.id.action_navigation_dashboard_to_shiftsFragment
            ),
            DashboardOption(
                title = getString(R.string.solicitud_cambio_turno),
                iconRes = R.drawable.ic_swap_24,
                navAction = R.id.action_navigation_dashboard_to_shiftChangeFragment
            ),
            DashboardOption(
                title = getString(R.string.solicitar_vacaciones),
                iconRes = R.drawable.ic_beach_24,
                navAction = R.id.action_navigation_dashboard_to_vacationsFragment
            )
        )

        binding.rvOptions.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvOptions.adapter = DashboardAdapter(options) { actionId ->
            findNavController().navigate(actionId)
        }

        binding.btnLogout.setOnClickListener {
            // TODO: tu lógica de logout (limpiar sesión, ir a login, etc.)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class DashboardOption(
    val title: String,
    val iconRes: Int,
    val navAction: Int
)
