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
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentHomeBinding
import com.sanna.provcalapp.ui.home.CheckInBottomSheet.Companion.KEY_CONFIRMED
import com.sanna.provcalapp.ui.home.CheckInBottomSheet.Companion.KEY_IS_INGRESO
import com.sanna.provcalapp.ui.home.CheckInBottomSheet.Companion.REQ_CHECKIN
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var checkedIn = false // estado demo

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 60_000L)
        }
    }

    private val localeEs = Locale("es", "ES")
    private val timeFmt = SimpleDateFormat("hh:mm a", localeEs)
    private val dateFmt = SimpleDateFormat("dd MMM, yyyy - EEEE", localeEs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // restaurar estado si el fragment se recrea
        checkedIn = savedInstanceState?.getBoolean("checkedIn") ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateClock()
        handler.postDelayed(tick, remainingMsToNextMinute())

        val innerButton = binding.root.findViewById<MaterialCardView>(R.id.innerButton)
        val icon = binding.root.findViewById<ImageView>(R.id.icTouch)
        val label = binding.root.findViewById<TextView>(R.id.labelAttendance)

        fun render() {
            if (checkedIn) {
                label.text = getString(R.string.salida)
                icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
            } else {
                label.text = getString(R.string.ingreso)
                icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#18A558"))
            }
        }
        render()

        // 💡 IMPORTANTE: escuchar resultados en el MISMO manager donde abrimos el dialog
        childFragmentManager.setFragmentResultListener(
            REQ_CHECKIN, viewLifecycleOwner
        ) { _, bundle ->
            val confirmed = bundle.getBoolean(KEY_CONFIRMED, false)
            val isIngreso = bundle.getBoolean(KEY_IS_INGRESO, true)
            if (confirmed) {
                checkedIn = isIngreso      // ingreso => true, salida => false
                render()
                innerButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        }

        innerButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            it.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90)
                .withEndAction { it.animate().scaleX(1f).scaleY(1f).setDuration(90).start() }
                .start()

            // Si no está dentro -> modal para INGRESO; si ya está dentro -> modal para SALIDA
            val isIngreso = !checkedIn
            CheckInBottomSheet.newInstance(isIngreso)
                .show(childFragmentManager, "CheckInBottomSheet")
        }
    }

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
