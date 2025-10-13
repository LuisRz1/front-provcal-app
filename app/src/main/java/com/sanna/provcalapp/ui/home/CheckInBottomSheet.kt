package com.sanna.provcalapp.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.sanna.provcalapp.R

class CheckInBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener { di ->
            val d = di as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_swipe_checkin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val actionIsIngreso = requireArguments().getBoolean(ARG_IS_INGRESO, true)

        // Views
        val tvInstruction = view.findViewById<TextView>(R.id.tvInstruction)
        val slider = view.findViewById<Slider>(R.id.sliderConfirm)
        val whiteCircle = view.findViewById<View>(R.id.whiteCircle)
        val chevrons = view.findViewById<TextView>(R.id.tvChevrons)
        val swipeContainer = view.findViewById<FrameLayout>(R.id.swipeContainer)
        val cardSwipe = view.findViewById<MaterialCardView>(R.id.cardSwipe)

        // Texto según acción
        tvInstruction.text = getString(
            if (actionIsIngreso) R.string.desliza_derecha_ingreso
            else R.string.desliza_derecha_salida
        )

        // Cuando el layout ya tiene tamaño, calculamos el rango de movimiento
        cardSwipe.post {
            val paddingLeft = swipeContainer.paddingStart
            val paddingRight = swipeContainer.paddingEnd
            val available = swipeContainer.width - paddingLeft - paddingRight - whiteCircle.width
            val maxTranslation = available.coerceAtLeast(0)

            // posición inicial
            whiteCircle.translationX = 0f
            chevrons.translationX = 0f

            // mover círculo & chevrons con el slider (invisible)
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val t = (value / slider.valueTo) * maxTranslation
                    whiteCircle.translationX = t
                    // centra los «»» dentro del círculo
                    chevrons.translationX = t + (whiteCircle.width - chevrons.width) / 2f - 2f
                }
            }

            val threshold = 95f
            slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}
                override fun onStopTrackingTouch(slider: Slider) {
                    if (slider.value >= threshold) {
                        // Confirmado
                        setFragmentResult(
                            REQ_CHECKIN,
                            bundleOf(KEY_CONFIRMED to true, KEY_IS_INGRESO to actionIsIngreso)
                        )
                        dismissAllowingStateLoss()
                    } else {
                        // Volver suave al inicio
                        whiteCircle.animate().translationX(0f).setDuration(160).start()
                        chevrons.animate().translationX(0f).setDuration(160).start()
                        slider.value = 0f
                    }
                }
            })
        }
    }

    companion object {
        const val REQ_CHECKIN = "checkin_request"
        const val KEY_CONFIRMED = "confirmed"
        const val KEY_IS_INGRESO = "is_ingreso"
        private const val ARG_IS_INGRESO = "arg_is_ingreso"

        fun newInstance(isIngreso: Boolean) = CheckInBottomSheet().apply {
            arguments = bundleOf(ARG_IS_INGRESO to isIngreso)
        }
    }
}
