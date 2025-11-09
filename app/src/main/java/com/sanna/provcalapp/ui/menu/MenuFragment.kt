package com.sanna.provcalapp.ui.menu

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentMenuBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

    // modelo en memoria para poder editar
    data class MenuDay(
        var desayuno: MutableList<String>,
        var almuerzo: MutableList<String>,
        var cena: MutableList<String>
    )

    // ahora sí es mutable
    private val menuData = mutableMapOf(
        1 to MenuDay(
            mutableListOf("Sánguche de pollo", "Café pasado", "Manzana"),
            mutableListOf("Arroz tapado", "Jugo de maracuyá", "Ceviche"),
            mutableListOf("Bistec a lo pobre", "Gaseosa", "Pie de manzana")
        ),
        2 to MenuDay(
            mutableListOf("Pan con palta", "Emoliente"),
            mutableListOf("Seco de res", "Arroz blanco"),
            mutableListOf("Sopa criolla")
        )
        // agrega más días si quieres
    )

    // Launcher para seleccionar archivos
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMonthNavigation()
        updateCalendar()

        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                )
            )
        }
        filePickerLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo.xlsx"
        binding.tvFileName.text = fileName
        Toast.makeText(requireContext(), "Archivo seleccionado: $fileName", Toast.LENGTH_SHORT)
            .show()
    }

    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
    }

    private fun updateCalendar() {
        binding.tvCurrentMonth.text = dateFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
        binding.gridCalendar.removeAllViews()

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until firstDayOfWeek) {
            addEmptyDayCell()
        }

        for (day in 1..daysInMonth) {
            addDayCell(day)
        }
    }

    private fun addEmptyDayCell() {
        val cell = View(requireContext()).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
        }
        binding.gridCalendar.addView(cell)
    }

    private fun addDayCell(day: Int) {
        val cellLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(6, 6, 6, 6)
            setBackgroundResource(R.drawable.bg_calendar_cell)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(3, 3, 3, 3)
            }
            isClickable = true
            isFocusable = true
        }

        val tvDay = TextView(requireContext()).apply {
            text = day.toString()
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 4)
        }
        cellLayout.addView(tvDay)

        // si hay datos, ponlos debajo
        val menu = menuData[day]
        menu?.desayuno?.forEach { item ->
            val tvItem = TextView(requireContext()).apply {
                text = item
                textSize = 8f
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.green_500
                    )
                )
                setPadding(6, 3, 6, 3)
                gravity = Gravity.CENTER
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 2, 0, 0)
                }
            }
            cellLayout.addView(tvItem)
        }

        cellLayout.setOnClickListener {
            showMenuDialog(day)
        }

        binding.gridCalendar.addView(cellLayout)
    }

    private fun showMenuDialog(day: Int) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_menu_day)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvDayNumber = dialog.findViewById<TextView>(R.id.tvDayNumber)
        val tvDayName = dialog.findViewById<TextView>(R.id.tvDayName)
        val btnClose = dialog.findViewById<TextView>(R.id.btnClose)

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, day)
        val dayOfWeek = SimpleDateFormat("EEE", Locale("es", "ES"))
            .format(tempCal.time).uppercase()

        tvDayNumber.text = String.format("%02d", day)
        tvDayName.text = dayOfWeek

        setupMenuItems(dialog, day)

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun setupMenuItems(dialog: Dialog, day: Int) {
        val data = menuData[day] ?: MenuDay(
            mutableListOf(),
            mutableListOf(),
            mutableListOf()
        ).also { menuData[day] = it }

        val layoutDesayuno = dialog.findViewById<LinearLayout>(R.id.layoutDesayuno)
        val btnCambiarDesayuno = dialog.findViewById<MaterialButton>(R.id.btnCambiarDesayuno)

        val layoutAlmuerzo = dialog.findViewById<LinearLayout>(R.id.layoutAlmuerzo)
        val btnCambiarAlmuerzo = dialog.findViewById<MaterialButton>(R.id.btnCambiarAlmuerzo)

        val layoutCena = dialog.findViewById<LinearLayout>(R.id.layoutCena)
        val btnCambiarCena = dialog.findViewById<MaterialButton>(R.id.btnCambiarCena)

        // listeners de toggle
        btnCambiarDesayuno.setOnClickListener {
            toggleEditMeal(
                layoutDesayuno,
                btnCambiarDesayuno,
                data.desayuno
            ) { newList ->
                data.desayuno = newList.toMutableList()
                Toast.makeText(requireContext(), "Desayuno actualizado", Toast.LENGTH_SHORT).show()
            }
        }

        btnCambiarAlmuerzo.setOnClickListener {
            toggleEditMeal(
                layoutAlmuerzo,
                btnCambiarAlmuerzo,
                data.almuerzo
            ) { newList ->
                data.almuerzo = newList.toMutableList()
                Toast.makeText(requireContext(), "Almuerzo actualizado", Toast.LENGTH_SHORT).show()
            }
        }

        btnCambiarCena.setOnClickListener {
            toggleEditMeal(
                layoutCena,
                btnCambiarCena,
                data.cena
            ) { newList ->
                data.cena = newList.toMutableList()
                Toast.makeText(requireContext(), "Cena actualizada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Cambia TextViews -> EditTexts y viceversa.
     * Cuando guarda, llama a onSave con la nueva lista.
     */
    private fun toggleEditMeal(
        layout: LinearLayout,
        button: MaterialButton,
        currentValues: List<String>,
        onSave: (List<String>) -> Unit
    ) {
        if (button.text.toString().contains("Proponer", ignoreCase = true)) {
            // pasar a modo edición
            for (i in 0 until layout.childCount) {
                val view = layout.getChildAt(i)
                if (view is TextView) {
                    val et = EditText(requireContext()).apply {
                        setText(view.text.toString())
                        textSize = 12f
                        setTextColor(Color.parseColor("#666666"))
                        setBackgroundColor(Color.parseColor("#E8E8E8"))
                        setPadding(8, 8, 8, 8)
                    }
                    layout.removeViewAt(i)
                    layout.addView(et, i)
                }
            }
            button.text = "Guardar"
            button.icon = null
        } else {
            // guardar y volver a modo lectura
            val newValues = mutableListOf<String>()
            for (i in 0 until layout.childCount) {
                val view = layout.getChildAt(i)
                if (view is EditText) {
                    val text = view.text.toString()
                    newValues.add(text)
                    val tv = TextView(requireContext()).apply {
                        this.text = text
                        textSize = 12f
                        setTextColor(Color.parseColor("#666666"))
                        setBackgroundColor(Color.parseColor("#E8E8E8"))
                        setPadding(8, 8, 8, 8)
                    }
                    layout.removeViewAt(i)
                    layout.addView(tv, i)
                }
            }
            onSave(newValues)
            button.text = "Proponer cambio"
            // si quieres volver a poner icono, aquí lo pones de nuevo
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
