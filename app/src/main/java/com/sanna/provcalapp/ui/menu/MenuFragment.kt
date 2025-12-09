package com.sanna.provcalapp.ui.menu

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
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
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sanna.provcalapp.MonthlyMenuQuery
import com.sanna.provcalapp.R
import com.sanna.provcalapp.databinding.FragmentMenuBinding
import com.sanna.provcalapp.type.MenuChangeItemInput
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val vm: MenuViewModel by viewModels()

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

    // Día (1..31) -> id de MenuDay para proponer cambios
    private val menuDayIdByDay = mutableMapOf<Int, String>()

    // Modelo simple para pintar en el grid
    data class MenuDay(
        var desayuno: MutableList<String>,
        var almuerzo: MutableList<String>,
        var cena: MutableList<String>
    )
    private val menuData = mutableMapOf<Int, MenuDay>()

    // File picker
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handleSelectedFile(uri) }
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

        // Observers básicos
        vm.message.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
        vm.menu.observe(viewLifecycleOwner) { menu ->
            if (menu != null) paintMenu(menu.days)
        }

        // Navegación de mes
        binding.btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
            queryBackend()
        }
        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
            queryBackend()
        }

        // Seleccionar archivo
        binding.btnSelectFile.setOnClickListener { openFilePicker() }

        // Inicio
        updateCalendar()
        queryBackend()
    }

    // ----- Backend → UI -----
    private fun paintMenu(days: List<MonthlyMenuQuery.Day>) {
        menuDayIdByDay.clear()
        menuData.clear()

        fun splitList(s: String?): MutableList<String> =
        (s ?: "")
            .trim()
            .trim('"') // por si viene entre comillas del CSV
            .split(Regex("\\s*[|,;]\\s*")) // separa por | o ,
            .filter { it.isNotEmpty() }
            .toMutableList()


        for (d in days) {
            val dayNum = try { d.date.substring(8, 10).toInt() } catch (_: Exception) { continue }
            menuDayIdByDay[dayNum] = d.id
            menuData[dayNum] = MenuDay(
                desayuno = splitList(d.breakfast),
                almuerzo = splitList(d.lunch),
                cena = splitList(d.dinner)
            )
        }
        updateCalendar() // repinta
    }

    // ----- UI de calendario -----
    private fun updateCalendar() {
        binding.tvCurrentMonth.text = dateFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
        binding.gridCalendar.removeAllViews()

        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH)

        repeat(firstDayOfWeek) { addEmptyDayCell() }
        for (day in 1..daysInMonth) addDayCell(day)
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

        // Pintar desayuno (ejemplo corto; repite si quieres para almuerzo/cena)
        val data = menuData[day]
        data?.desayuno?.forEach { item ->
            val tvItem = TextView(requireContext()).apply {
                text = item
                textSize = 8f
                setTextColor(Color.WHITE)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_500))
                setPadding(6, 3, 6, 3)
                gravity = Gravity.CENTER
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 2, 0, 0) }
            }
            cellLayout.addView(tvItem)
        }

        cellLayout.setOnClickListener { showMenuDialog(day) }
        binding.gridCalendar.addView(cellLayout)
    }

    // ----- Diálogo día -----
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

        val tmp = calendar.clone() as Calendar
        tmp.set(Calendar.DAY_OF_MONTH, day)
        val dayOfWeek = SimpleDateFormat("EEE", Locale("es", "ES")).format(tmp.time).uppercase()

        tvDayNumber.text = String.format("%02d", day)
        tvDayName.text = dayOfWeek

        setupMenuItems(dialog, day)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun buildMealItemView(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            setBackgroundColor(Color.parseColor("#E8E8E8"))
            setPadding(8, 8, 8, 8)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, 6)
            layoutParams = lp
        }
    }

    private fun renderMeal(layout: LinearLayout, items: List<String>) {
        layout.removeAllViews()
        val list = if (items.isEmpty()) listOf("—") else items
        list.forEach { layout.addView(buildMealItemView(it)) }
    }

    private fun setupMenuItems(dialog: Dialog, day: Int) {
        val layoutDesayuno = dialog.findViewById<LinearLayout>(R.id.layoutDesayuno)
        val btnCambiarDesayuno = dialog.findViewById<MaterialButton>(R.id.btnCambiarDesayuno)
        val layoutAlmuerzo = dialog.findViewById<LinearLayout>(R.id.layoutAlmuerzo)
        val btnCambiarAlmuerzo = dialog.findViewById<MaterialButton>(R.id.btnCambiarAlmuerzo)
        val layoutCena = dialog.findViewById<LinearLayout>(R.id.layoutCena)
        val btnCambiarCena = dialog.findViewById<MaterialButton>(R.id.btnCambiarCena)

        // RELLENAR CON LOS DATOS QUE YA LLEGARON DEL BACKEND
        val dayData = menuData[day]
        renderMeal(layoutDesayuno, dayData?.desayuno ?: emptyList())
        renderMeal(layoutAlmuerzo, dayData?.almuerzo ?: emptyList())
        renderMeal(layoutCena,     dayData?.cena     ?: emptyList())

        // Botones de edición / propuesta de cambio
        btnCambiarDesayuno.setOnClickListener {
            toggleEditMeal(layoutDesayuno, btnCambiarDesayuno) { nuevos ->
                proposeFor(day, "breakfast", nuevos.joinToString(", "))
            }
        }
        btnCambiarAlmuerzo.setOnClickListener {
            toggleEditMeal(layoutAlmuerzo, btnCambiarAlmuerzo) { nuevos ->
                proposeFor(day, "lunch", nuevos.joinToString(", "))
            }
        }
        btnCambiarCena.setOnClickListener {
            toggleEditMeal(layoutCena, btnCambiarCena) { nuevos ->
                proposeFor(day, "dinner", nuevos.joinToString(", "))
            }
        }
    }

    /** Cambia TextViews <-> EditTexts. onSave recibe la lista final. */
    private fun toggleEditMeal(
        layout: LinearLayout,
        button: MaterialButton,
        onSave: (List<String>) -> Unit
    ) {
        if (button.text.toString().contains("Proponer", ignoreCase = true)) {
            // a edición
            for (i in 0 until layout.childCount) {
                val v = layout.getChildAt(i)
                if (v is TextView) {
                    val et = EditText(requireContext()).apply {
                        setText(v.text.toString())
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
        } else {
            // guardar
            val newValues = mutableListOf<String>()
            for (i in 0 until layout.childCount) {
                val v = layout.getChildAt(i)
                if (v is EditText) {
                    val text = v.text.toString().trim()
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
        }
    }

    // ----- File picker -----
    // ----- File picker -----
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    // Excel
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    // CSV (variantes reales que reportan muchos proveedores)
                    "text/csv",
                    "text/comma-separated-values",
                    "text/x-comma-separated-values",
                    "text/plain",
                    "application/csv",
                    "application/x-csv",
                    "application/octet-stream"
                )
            )
        }
        filePickerLauncher.launch(intent)
    }


    private fun handleSelectedFile(uri: Uri) {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val fileName = getFileName(uri) ?: "menu_${y}_${m}.xlsx"
        val base64 = readAsBase64(uri) ?: run {
            Toast.makeText(requireContext(), "No se pudo leer el archivo", Toast.LENGTH_SHORT).show()
            return
        }
        binding.tvFileName.text = fileName

        vm.uploadMenu(y, m, fileName, base64, overwrite = false) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sobrescribir menú")
                .setMessage("Ya existe un menú para este mes. ¿Deseas reemplazarlo?")
                .setPositiveButton("Sí, reemplazar") { _, _ ->
                    vm.uploadMenu(y, m, fileName, base64, overwrite = true) { }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use { if (it.moveToFirst()) name = it.getString(0) }
        return name
    }

    private fun readAsBase64(uri: Uri): String? {
        return try {
            val input: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val bytes = input?.use { it.readBytes() } ?: return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    // ----- Proponer cambio -----
    private fun proposeFor(day: Int, mealType: String, newValueJoined: String) {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val menuDayId = menuDayIdByDay[day] ?: run {
            Toast.makeText(requireContext(), "No hay menú para este día", Toast.LENGTH_SHORT).show()
            return
        }
        val dateIso = String.format(Locale.US, "%04d-%02d-%02d", y, m, day)

        val item = MenuChangeItemInput(
            menuDayId = menuDayId,
            day = dateIso, // mapeado a scalar Date (string)
            mealType = mealType,
            newValue = newValueJoined,
            reason = "Actualizado por Nutricionista desde app",
            emergency = false
        )
        vm.proposeChanges(listOf(item)) {
            vm.loadMenu(y, m) // refresca
        }
    }

    private fun queryBackend() {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        vm.loadMenu(y, m)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
