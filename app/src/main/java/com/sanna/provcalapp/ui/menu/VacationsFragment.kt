package com.sanna.provcalapp.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentVacationsBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlin.math.ceil

class VacationsFragment : Fragment() {

    private var _binding: FragmentVacationsBinding? = null
    private val binding get() = _binding!!

    private val localeEs = Locale("es", "ES")
    private val dateFmt = SimpleDateFormat("dd MMM yyyy", localeEs)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVacationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 1) Adapters para desplegables
        val tiposSolicitud = listOf("Vacaciones", "Día libre personal", "Licencia médica")
        val diasDisponibles = (1..7).map { "$it día${if (it > 1) "s" else ""}" }

        binding.etTipo.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tiposSolicitud)
        )
        binding.etDias.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, diasDisponibles)
        )

        // Abre el dropdown al enfocar (UX)
        binding.etTipo.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.etTipo.showDropDown() }
        binding.etDias.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.etDias.showDropDown() }

        // 2) DateRangePicker para Inicio/Fin
        val rangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.selecciona_rango))
            .build()

        fun openRangePicker() {
            if (!rangePicker.isAdded) rangePicker.show(parentFragmentManager, "rangePicker")
        }

        // Clicks en icono y en los campos
        binding.tilInicio.setEndIconOnClickListener { openRangePicker() }
        binding.tilFin.setEndIconOnClickListener { openRangePicker() }
        binding.etInicio.setOnClickListener { openRangePicker() }
        binding.etFin.setOnClickListener { openRangePicker() }

        rangePicker.addOnPositiveButtonClickListener { sel ->
            val start = sel.first
            val end = sel.second
            if (start != null) binding.etInicio.setText(dateFmt.format(Date(start)))
            if (end != null) binding.etFin.setText(dateFmt.format(Date(end)))

            // Validar rango vs. "Días" elegidos
            validateDaysVsRange(start, end)
            validateForm()
        }

        // 3) Revalidaciones
        binding.etTipo.setOnItemClickListener { _, _, _, _ -> validateForm() }
        binding.etDias.setOnItemClickListener { _, _, _, _ ->
            // Si ya hay un rango, revalida longitud vs días elegidos
            val start = parseDateMillis(binding.etInicio.text?.toString())
            val end = parseDateMillis(binding.etFin.text?.toString())
            validateDaysVsRange(start, end)
            validateForm()
        }
        binding.etMotivo.addTextChangedListener { validateForm() }

        // 4) Enviar
        binding.btnSolicitar.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.solicitud_enviada), Toast.LENGTH_SHORT).show()
            // TODO: enviar al backend
        }
    }

    // --- Helpers ---
    private fun validateForm() {
        val tipoOk = !binding.etTipo.text.isNullOrBlank()
        val diasOk = !binding.etDias.text.isNullOrBlank()
        val inicioOk = !binding.etInicio.text.isNullOrBlank()
        val finOk = !binding.etFin.text.isNullOrBlank()
        val motivoOk = (binding.etMotivo.text?.length ?: 0) >= 3

        binding.tilTipo.error = if (tipoOk) null else getString(R.string.requerido)
        binding.tilDias.error = if (diasOk) null else getString(R.string.requerido)
        binding.tilInicio.error = if (inicioOk) null else getString(R.string.requerido)
        binding.tilFin.error = if (finOk) null else getString(R.string.requerido)
        binding.tilMotivo.error = if (motivoOk) null else getString(R.string.minimo_5)

        val rangeOk = binding.tilFin.error.isNullOrEmpty() && binding.tilInicio.error.isNullOrEmpty()
        binding.btnSolicitar.isEnabled = tipoOk && diasOk && inicioOk && finOk && motivoOk && rangeOk
    }

    private fun validateDaysVsRange(start: Long?, end: Long?) {
        // Si no hay rango completo, limpia errores y sale
        if (start == null || end == null) {
            binding.tilInicio.error = null
            binding.tilFin.error = null
            return
        }
        val msPerDay = 86_400_000L
        // Cálculo inclusivo (p.ej., 10–12 = 3 días)
        val selectedDays = ceil((end - start).toDouble() / msPerDay).toInt() + 1

        // Extraer número elegido en "Días"
        val chosenDays = binding.etDias.text?.toString()
            ?.takeWhile { it.isDigit() }
            ?.toIntOrNull()

        // Límite duro a 7 por si cambian el adapter
        if (selectedDays > 7) {
            binding.tilFin.error = getString(R.string.max_7_dias)
            return
        }

        if (chosenDays != null && selectedDays > chosenDays) {
            binding.tilFin.error = getString(R.string.rango_supera_dias, chosenDays)
        } else {
            binding.tilFin.error = null
        }
    }

    // Convierte texto mostrado en etInicio/etFin a millis (mismo formato con el que seteamos)
    private fun parseDateMillis(text: String?): Long? = try {
        if (text.isNullOrBlank()) null else dateFmt.parse(text)?.time
    } catch (_: Exception) { null }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
