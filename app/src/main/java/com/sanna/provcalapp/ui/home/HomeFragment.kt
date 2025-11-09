// File: ui/home/HomeFragment.kt
package com.sanna.provcalapp.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentHomeBinding
import com.sanna.provcalapp.ui.home.CheckInBottomSheet.Companion.KEY_CONFIRMED
import com.sanna.provcalapp.ui.home.CheckInBottomSheet.Companion.KEY_IS_INGRESO
import com.sanna.provcalapp.ui.home.CheckInBottomSheet.Companion.REQ_CHECKIN
import com.sanna.provcalapp.data.models.AttendanceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            // "The binding cannot be accessed. Is the view null?"
        }

    // ViewModel
    private val viewModel: HomeViewModel by viewModels()
    // States
    private var checkedIn = false
    private var isOnBreak = false

    // UI Handlers
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 60_000L)
        }
    }

    // Formato para fecha y hora
    private val localeEs = Locale("es", "ES")
    private val timeFmt = SimpleDateFormat("hh:mm a", localeEs)
    private val dateFmt = SimpleDateFormat("dd MMM, yyyy - EEEE", localeEs)

    // Permisos de ubicación
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Permiso concedido, ejecutar acción pendiente
                executePendingAction()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Permiso básico concedido
                executePendingAction()
            }
            else -> {
                showError("Se requieren permisos de ubicación para marcar asistencia")
            }
        }
    }

    private var pendingAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkedIn = savedInstanceState?.getBoolean("checkedIn") ?: false
        isOnBreak = savedInstanceState?.getBoolean("isOnBreak") ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        setupUI()
        setupObservers()
        updateClock()
        handler.postDelayed(tick, remainingMsToNextMinute())
    }

    private fun setupUI() {
        // Configurar info del usuario
        updateUserInfo()

        // Configurar botón de asistencia
        val innerButton = binding.root.findViewById<MaterialCardView>(R.id.innerButton)
        val breakButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBreak)
        val logoutButton = binding.root.findViewById<ImageView>(R.id.btnLogout)

        renderAttendanceButton()

        // Click en botón de logout
        logoutButton?.setOnClickListener {
            showLogoutConfirmation()
        }

        // Listener del resultado del bottom sheet
        childFragmentManager.setFragmentResultListener(
            REQ_CHECKIN, viewLifecycleOwner
        ) { _, bundle ->
            val confirmed = bundle.getBoolean(KEY_CONFIRMED, false)
            val isIngreso = bundle.getBoolean(KEY_IS_INGRESO, true)

            if (confirmed) {
                handleAttendanceAction(isIngreso)
            }
        }

        // Click en el botón de asistencia (solo check-in/check-out)
        innerButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            it.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                }
                .start()

            // Determinar qué acción mostrar (solo ingreso o salida)
            val action = if (!checkedIn) AttendanceAction.CHECK_IN else AttendanceAction.CHECK_OUT
            CheckInBottomSheet.newInstance(action)
                .show(childFragmentManager, "CheckInBottomSheet")
        }

        // Click en el botón de descanso
        breakButton?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            if (isOnBreak) {
                // Finalizar descanso
                checkLocationPermissionAndExecute {
                    viewModel.endBreak()
                }
            } else {
                // Iniciar descanso
                checkLocationPermissionAndExecute {
                    viewModel.startBreak()
                }
            }
        }
    }

    private fun setupObservers() {
        // Observar estado de la asistencia
        viewModel.attendanceState.observe(viewLifecycleOwner) { state ->
            checkedIn = state.isCheckedIn
            isOnBreak = state.isOnBreak

            // Actualizar UI
            renderAttendanceButton()
            updateStatsUI(state)
        }

        // Observar loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }

        // Observar mensajes
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                showMessage(message)
            }
        }

        // Observar info del usuario
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.root.findViewById<TextView>(R.id.txtGreeting)?.text =
                "¡HOLA! ${name.uppercase()}"
        }

        viewModel.employeeId.observe(viewLifecycleOwner) { employeeId ->
            binding.root.findViewById<TextView>(R.id.txtUserCode)?.text = employeeId
        }
    }

    private fun updateUserInfo() {
        // Esta info ya viene del ViewModel a través de los observers
    }

    private fun determineAction(): AttendanceAction {
        return when {
            !checkedIn -> AttendanceAction.CHECK_IN
            isOnBreak -> AttendanceAction.END_BREAK
            checkedIn && !isOnBreak -> AttendanceAction.START_BREAK_OR_CHECK_OUT
            else -> AttendanceAction.CHECK_IN
        }
    }

    // Check-in/Check-out handler
    private fun handleAttendanceAction(isIngreso: Boolean) {
        when {
            isIngreso && !checkedIn -> {
                // Check-in (Ingreso)
                checkLocationPermissionAndExecute {
                    viewModel.checkIn()
                }
            }
            !isIngreso && checkedIn -> {
                // Check-out (Salida)
                checkLocationPermissionAndExecute {
                    viewModel.checkOut()
                }
            }
        }
    }

    private fun checkLocationPermissionAndExecute(action: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Ya tenemos permiso, ejecutar acción
                action()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Mostrar explicación
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Permiso de Ubicación")
                    .setMessage("Esta aplicación necesita acceso a tu ubicación para verificar que estés en el lugar de trabajo al marcar tu asistencia.")
                    .setPositiveButton("Entendido") { _, _ ->
                        pendingAction = action
                        requestLocationPermission()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> {
                // Solicitar permiso directamente
                pendingAction = action
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun executePendingAction() {
        pendingAction?.invoke()
        pendingAction = null
    }

    private fun renderAttendanceButton() {
        val icon = binding.root.findViewById<ImageView>(R.id.icTouch)
        val label = binding.root.findViewById<TextView>(R.id.labelAttendance)
        val breakButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBreak)

        when {
            !checkedIn -> {
                // Estado: No ha marcado ingreso
                label.text = getString(R.string.ingreso)
                icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#18A558"))
                icon.setImageResource(R.drawable.ic_touch)
                label.visibility = View.VISIBLE
                breakButton?.visibility = View.GONE
            }
            isOnBreak -> {
                // Estado: En descanso - mostrar icono de café
                label.visibility = View.GONE
                icon.setImageResource(R.drawable.coffe_time)
                icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#8B4513")) // Brown color for coffee

                // Botón de descanso: "Regresar al trabajo"
                breakButton?.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.regresar_al_trabajo)
                    setIconResource(android.R.drawable.ic_media_play)
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981")) // Verde
                }
            }
            checkedIn -> {
                // Estado: Ya marcó ingreso, puede marcar salida
                label.text = getString(R.string.salida)
                label.visibility = View.VISIBLE
                icon.setImageResource(R.drawable.ic_touch)
                icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))

                // Botón de descanso: "Toma un break"
                breakButton?.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.toma_un_break)
                    setIconResource(R.drawable.coffee)
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6")) // Azul
                }
            }
        }
    }

    // Displays the employee's check-in and check-out time
    private fun updateStatsUI(state: AttendanceState) {
        Log.d("HomeStats", "updateStatsUI() -> checkInTime = ${state.checkInTime}, checkOutTime = ${state.checkOutTime}, totalHours = ${state.totalHours}")
        // Update check-in time
        binding.root.findViewById<TextView>(R.id.txtHoraIngreso)?.text =
            state.checkInTime ?: "--:--"

        // Update check-out time
        binding.root.findViewById<TextView>(R.id.txtHoraSalida)?.text =
            state.checkOutTime ?: "--:--"

        // Update total work hours
        binding.root.findViewById<TextView>(R.id.txtHorasTotales)?.text =
            state.totalHours ?: "--:--"
    }

    private fun showLoading(isLoading: Boolean) {
        // Puedes agregar un ProgressBar en tu layout si quieres
        // Por ahora, deshabilitamos el botón
        val innerButton = binding.root.findViewById<MaterialCardView>(R.id.innerButton)
        innerButton.isEnabled = !isLoading
        innerButton.alpha = if (isLoading) 0.5f else 1.0f
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Displays the date and time of the day
    private fun updateClock() {
        val now = Date()
        binding.txtTime.text = timeFmt.format(now).uppercase(localeEs)
        binding.txtDate.text = dateFmt.format(now)
    }

    private fun remainingMsToNextMinute(): Long {
        val now = System.currentTimeMillis()
        return 60_000L - (now % 60_000L)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("checkedIn", checkedIn)
        outState.putBoolean("isOnBreak", isOnBreak)
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logout_confirm_title))
            .setMessage(getString(R.string.logout_confirm_message))
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                performLogout()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun performLogout() {
        // Clear all state
        viewModel.logout()

        // Navigate to login screen
        val navController = androidx.navigation.fragment.NavHostFragment.findNavController(this)
        navController.navigate(R.id.action_homeFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}

// Enum para las acciones posibles
enum class AttendanceAction {
    CHECK_IN,
    CHECK_OUT,
    START_BREAK,
    END_BREAK,
    START_BREAK_OR_CHECK_OUT
}