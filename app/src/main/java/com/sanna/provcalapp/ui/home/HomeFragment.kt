package com.sanna.provcalapp.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.sanna.provcalapp.R
import com.sanna.provcalapp.attendance.data.AttendanceRepository
import com.sanna.provcalapp.core.network.Graphql
import com.sanna.provcalapp.core.network.GraphqlClient
import com.sanna.provcalapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var checkedIn = false

    // Reloj en vivo
    private val localeEsPe = Locale("es", "PE")
    private val timeFmt = SimpleDateFormat("hh:mm a", localeEsPe)
    private val dateFmt = SimpleDateFormat("dd MMM, yyyy - EEEE", localeEsPe)

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkedIn = savedInstanceState?.getBoolean("checkedIn") ?: false

        // Asegúrate de setear el token VIGENTE una sola vez
        if (Graphql.ACCESS_TOKEN == null) {
            Graphql.ACCESS_TOKEN = "PEGAR_AQUI_TU_TOKEN_VIGENTE"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nombre y email reales (token)
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { GraphqlClient().currentUser() }
            binding.homeHeader.tvUserName.text = user?.fullName ?: getString(R.string.app_name)
            binding.homeHeader.tvUserCode.text = user?.email ?: ""
        }

        // Reloj en vivo
        updateClock()
        handler.post(tick)

        // Botón grande
        val innerButton: MaterialCardView = view.findViewById(R.id.innerButton)
        val icon: ImageView = view.findViewById(R.id.icTouch)
        val label: TextView = view.findViewById(R.id.labelAttendance)
        renderButtonState(icon, label)

        // Listener para el resultado del bottom sheet
        childFragmentManager.setFragmentResultListener(
            CheckInBottomSheet.REQ_CHECKIN, viewLifecycleOwner
        ) { _, bundle ->
            val confirmed = bundle.getBoolean(CheckInBottomSheet.KEY_CONFIRMED, false)
            val isIngreso = bundle.getBoolean(CheckInBottomSheet.KEY_IS_INGRESO, true)
            if (!confirmed) return@setFragmentResultListener

            if (isIngreso) {
                // ====== INGRESO ======
                lifecycleScope.launch(Dispatchers.IO) {
                    val hora = AttendanceRepository().doCheckIn()
                    withContext(Dispatchers.Main) {
                        if (hora != null) {
                            binding.homeStats.txtHoraIngreso.text = hora
                            checkedIn = true
                            renderButtonState(icon, label)
                        } else {
                            Toast.makeText(requireContext(),
                                "No se pudo registrar ingreso (token/location?)",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // ====== SALIDA ======
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = AttendanceRepository().doCheckOut()
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            val (horaSalida, totalHHmm) = result
                            binding.homeStats.txtHoraSalida.text  = horaSalida
                            binding.homeStats.txtHorasTotales.text = totalHHmm
                            checkedIn = false
                            renderButtonState(icon, label)
                            Toast.makeText(requireContext(),
                                "Salida registrada",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(),
                                "No se pudo registrar salida (token/location?)",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // feedback háptico
            innerButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }

        innerButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            it.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90)
                .withEndAction { it.animate().scaleX(1f).scaleY(1f).setDuration(90).start() }
                .start()

            CheckInBottomSheet.newInstance(!checkedIn)
                .show(childFragmentManager, "CheckInBottomSheet")
        }

        // Limpia textos demo al inicio
        binding.homeStats.txtHoraIngreso.text = "—"
        binding.homeStats.txtHoraSalida.text  = "—"
        binding.homeStats.txtHorasTotales.text = "—"
    }

    private fun renderButtonState(icon: ImageView, label: TextView) {
        if (checkedIn) {
            label.text = getString(R.string.salida)
            icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
        } else {
            label.text = getString(R.string.ingreso)
            icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#18A558"))
        }
    }

    private fun updateClock() {
        val now = Date()
        binding.txtTime.text = timeFmt.format(now).uppercase(localeEsPe)
        val dateText = dateFmt.format(now)
        binding.txtDate.text = dateText.replaceFirstChar { it.titlecase(localeEsPe) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("checkedIn", checkedIn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
