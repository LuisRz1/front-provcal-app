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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sanna.provcalapp.MonthlyMenuQuery
import com.sanna.provcalapp.R
import com.sanna.provcalapp.type.MenuChangeItemInput
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MenuFragment : Fragment() {

    private val vm: MenuViewModel by viewModels()
    private val calendar = Calendar.getInstance()

    private var currentView: View? = null
    private var isUploadView = true

    // Día seleccionado actualmente
    private var selectedDay: MonthlyMenuQuery.Day? = null
    private var currentMeal = "breakfast" // breakfast, lunch, dinner

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
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observers
        vm.message.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        vm.menu.observe(viewLifecycleOwner) { menu ->
            if (menu != null && !isUploadView) {
                if (menu.days.isNotEmpty() && selectedDay == null) {
                    selectedDay = menu.days.first()
                }
                currentView?.let { setupCalendarDaySelection(it, menu) }
                renderMenuDayCard()
            }
        }

        // Vista inicial: carga de menú (subir Excel)
        showUploadView()
    }

    // ===== UPLOAD VIEW =====
    private fun showUploadView() {
        isUploadView = true
        val container = view?.findViewById<ViewGroup>(R.id.menuContainer) ?: return
        container.removeAllViews()

        currentView = layoutInflater.inflate(R.layout.layout_menu_upload, container, false)
        container.addView(currentView)

        setupUploadView(currentView!!)
    }

    private fun setupUploadView(view: View) {
        // Year spinner
        val acYear = view.findViewById<AutoCompleteTextView>(R.id.acYear)
        val years = (2024..2030).map { it.toString() }
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, years)
        acYear.setAdapter(yearAdapter)
        acYear.setText(calendar.get(Calendar.YEAR).toString(), false)

        // Month spinner
        val acMonth = view.findViewById<AutoCompleteTextView>(R.id.acMonth)
        val months = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, months)
        acMonth.setAdapter(monthAdapter)
        acMonth.setText(months[calendar.get(Calendar.MONTH)], false)

        // File selection
        val cardFileArea = view.findViewById<View>(R.id.cardFileArea)
        val btnSelectFile =
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectFile)

        cardFileArea.setOnClickListener { openFilePicker() }
        btnSelectFile.setOnClickListener { openFilePicker() }

        // Back navigation
        view.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            findNavController().navigateUp()
        }

        // Close session
        view.findViewById<LinearLayout>(R.id.btnCloseSession)?.setOnClickListener {
            performLogout()
        }
    }

    // ===== LIST VIEW =====
    private fun showListView() {
        isUploadView = false
        val container = view?.findViewById<ViewGroup>(R.id.menuContainer) ?: return
        container.removeAllViews()

        currentView = layoutInflater.inflate(R.layout.layout_menu_list, container, false)
        container.addView(currentView)

        setupListView(currentView!!)
        loadMenuForCurrentMonth()
    }

    private fun setupListView(view: View) {
        // Month display
        val tvCurrentMonth = view.findViewById<TextView>(R.id.tvCurrentMonth)
        updateMonthDisplay(tvCurrentMonth)

        // Month selector
        view.findViewById<LinearLayout>(R.id.layoutMonthSelector)?.setOnClickListener {
            showMonthPicker(tvCurrentMonth)
        }

        // Back to upload
        view.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            showUploadView()
        }

        // Close session
        view.findViewById<LinearLayout>(R.id.btnCloseSession)?.setOnClickListener {
            performLogout()
        }
        // El calendario se arma cuando llega el menú (en vm.menu.observe)
    }

    // ===== CALENDARIO (DÍAS DEL MES) =====
    private fun setupCalendarDaySelection(view: View, menu: MonthlyMenuQuery.Menu) {
        val row = view.findViewById<LinearLayout>(R.id.layoutDaysRow) ?: return
        row.removeAllViews()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        // Obtener cantidad de días del mes actual
        val tmpCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }
        val daysInMonth = tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (dayNum in 1..daysInMonth) {
            val dateIso = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayNum)

            // Buscar si hay menú para esa fecha
            val dayData = menu.days.firstOrNull { it.date.startsWith(dateIso) }

            val isSelected = selectedDay?.date?.startsWith(dateIso) == true
            val hasMenu = dayData != null

            val card = MaterialCardView(requireContext()).apply {
                val lp = LinearLayout.LayoutParams(
                    dpToPx(40),
                    dpToPx(40)
                )
                lp.marginEnd = dpToPx(8)
                layoutParams = lp

                radius = dpToPx(20).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(
                    when {
                        isSelected -> Color.parseColor("#18A558") // verde seleccionado
                        else -> Color.TRANSPARENT
                    }
                )

                val tv = TextView(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    text = dayNum.toString()
                    gravity = Gravity.CENTER
                    textSize = 14f
                    setTextColor(
                        when {
                            isSelected -> Color.WHITE
                            hasMenu -> Color.parseColor("#202124")
                            else -> Color.parseColor("#BDBDBD")
                        }
                    )
                }
                addView(tv)

                setOnClickListener {
                    if (!hasMenu) {
                        Toast.makeText(
                            requireContext(),
                            "No hay menú registrado para este día",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    selectedDay = dayData
                    currentMeal = "breakfast"
                    currentView?.let { v -> setupCalendarDaySelection(v, menu) }
                    renderMenuDayCard()
                }
            }

            row.addView(card)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun renderMenuDayCard() {
        val day = selectedDay ?: return
        val container = currentView?.findViewById<LinearLayout>(R.id.containerMenuDay) ?: return

        container.removeAllViews()

        val cardView = layoutInflater.inflate(R.layout.layout_menu_day_card, container, false)

        // Parse meals
        val breakfast = day.breakfast?.split("|")?.map { it.trim() } ?: emptyList()
        val lunch = day.lunch?.split("|")?.map { it.trim() } ?: emptyList()
        val dinner = day.dinner?.split("|")?.map { it.trim() } ?: emptyList()

        when (currentMeal) {
            "breakfast" -> {
                cardView.findViewById<TextView>(R.id.tvBebida)?.text =
                    breakfast.getOrNull(0) ?: "—"
                cardView.findViewById<TextView>(R.id.tvBebidaCalorias)?.text =
                    breakfast.getOrNull(1) ?: "—"
            }
            "lunch" -> {
                cardView.findViewById<TextView>(R.id.tvPlato)?.text =
                    lunch.getOrNull(0) ?: "—"
                cardView.findViewById<TextView>(R.id.tvPlatoCalorias)?.text =
                    lunch.getOrNull(1) ?: "—"
            }
            "dinner" -> {
                cardView.findViewById<TextView>(R.id.tvSandwich)?.text =
                    dinner.getOrNull(0) ?: "—"
                cardView.findViewById<TextView>(R.id.tvSandwichCalorias)?.text =
                    dinner.getOrNull(1) ?: "—"
            }
        }

        // Botones de tipo de comida
        val btnDesayuno =
            cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDesayuno)
        val btnAlmuerzo =
            cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAlmuerzo)
        val btnCena =
            cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCena)

        btnDesayuno?.setOnClickListener {
            currentMeal = "breakfast"
            renderMenuDayCard()
        }

        btnAlmuerzo?.setOnClickListener {
            currentMeal = "lunch"
            renderMenuDayCard()
        }

        btnCena?.setOnClickListener {
            currentMeal = "dinner"
            renderMenuDayCard()
        }

        // Edit button
        cardView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabEdit)
            ?.setOnClickListener {
                showEditDialog(day)
            }

        container.addView(cardView)
    }

    private fun updateMonthDisplay(textView: TextView) {
        textView.text = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
            .format(calendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    // ===== FILE HANDLING =====
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    "text/csv",
                    "text/plain"
                )
            )
        }
        filePickerLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        val yearText =
            currentView?.findViewById<AutoCompleteTextView>(R.id.acYear)?.text?.toString()
        val monthText =
            currentView?.findViewById<AutoCompleteTextView>(R.id.acMonth)?.text?.toString()

        val y = yearText?.toIntOrNull() ?: return
        val months = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val m = months.indexOf(monthText) + 1
        if (m <= 0) return

        val fileName = getFileName(uri) ?: "menu_${y}_${m}.xlsx"
        val base64 = readAsBase64(uri) ?: run {
            Toast.makeText(requireContext(), "No se pudo leer el archivo", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Popup de confirmación de importación
        showImportDialog(y, m, fileName, base64)
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

    // ===== POPUP IMPORT EXCEL =====
    private fun showImportDialog(
        year: Int,
        month: Int,
        fileName: String,
        base64: String
    ) {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.popup_import_excel, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvDialogFileName =
            dialogView.findViewById<TextView>(R.id.tvDialogFileName)
        val btnImport =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogImport)
        val btnCancel =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogCancel)

        tvDialogFileName.text = fileName

        btnImport.setOnClickListener {
            vm.uploadMenu(year, month, fileName, base64, overwrite = false) { conflict ->
                dialog.dismiss()

                if (conflict) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Sobrescribir menú")
                        .setMessage("Ya existe un menú para este mes. ¿Deseas reemplazarlo?")
                        .setPositiveButton("Sí, reemplazar") { _, _ ->
                            vm.uploadMenu(
                                year,
                                month,
                                fileName,
                                base64,
                                overwrite = true
                            ) { _ ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month - 1)
                                showListView()
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month - 1)
                    showListView()
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // ===== MONTH PICKER =====
    private fun showMonthPicker(textView: TextView) {
        val months = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar mes")
            .setItems(months) { _, which ->
                calendar.set(Calendar.MONTH, which)
                updateMonthDisplay(textView)
                loadMenuForCurrentMonth()
            }
            .show()
    }

    // ===== DATA LOADING =====
    private fun loadMenuForCurrentMonth() {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        vm.loadMenu(y, m)
    }

    // ===== EDIT DIALOG =====
    private fun showEditDialog(day: MonthlyMenuQuery.Day) {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.popup_edit_menu, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val acBebida = dialogView.findViewById<AutoCompleteTextView>(R.id.acBebida)
        val acPlato = dialogView.findViewById<AutoCompleteTextView>(R.id.acPlato)
        val acGuarnicion1 = dialogView.findViewById<AutoCompleteTextView>(R.id.acGuarnicion1)
        val acGuarnicion2 = dialogView.findViewById<AutoCompleteTextView>(R.id.acGuarnicion2)
        val acPanCon = dialogView.findViewById<AutoCompleteTextView>(R.id.acPanCon)
        val tvQuantity = dialogView.findViewById<TextView>(R.id.tvQuantity)

        val breakfast = day.breakfast?.split("|")?.map { it.trim() } ?: emptyList()
        val lunch = day.lunch?.split("|")?.map { it.trim() } ?: emptyList()
        val dinner = day.dinner?.split("|")?.map { it.trim() } ?: emptyList()

        if (breakfast.isNotEmpty()) acBebida.setText(breakfast.first())
        if (lunch.isNotEmpty()) acPlato.setText(lunch.first())
        if (lunch.size > 1) acGuarnicion2.setText(lunch.last())

        var quantity = 2
        if (dinner.isNotEmpty()) {
            val dinnerText = dinner.first()
            val parts = dinnerText.split(" ")
            quantity = parts.firstOrNull()?.toIntOrNull() ?: 2
            acPanCon.setText(parts.drop(3).joinToString(" "))
        }
        tvQuantity.text = quantity.toString()

        dialogView.findViewById<ImageView>(R.id.btnDecrease).setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
            }
        }

        dialogView.findViewById<ImageView>(R.id.btnIncrease).setOnClickListener {
            quantity++
            tvQuantity.text = quantity.toString()
        }

        dialogView.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirm)
            .setOnClickListener {
                val newBreakfast = acBebida.text.toString()
                val newLunch = "${acPlato.text} | ${acGuarnicion2.text}"
                val newDinner = "$quantity panes con ${acPanCon.text}"

                val y = calendar.get(Calendar.YEAR)
                val m = calendar.get(Calendar.MONTH) + 1
                val dayNum = day.date.substring(8, 10).toInt()
                val dateIso = String.format(Locale.US, "%04d-%02d-%02d", y, m, dayNum)

                val items = listOf(
                    MenuChangeItemInput(
                        menuDayId = day.id,
                        day = dateIso,
                        mealType = "breakfast",
                        newValue = newBreakfast,
                        reason = "Actualizado por Nutricionista",
                        emergency = false
                    ),
                    MenuChangeItemInput(
                        menuDayId = day.id,
                        day = dateIso,
                        mealType = "lunch",
                        newValue = newLunch,
                        reason = "Actualizado por Nutricionista",
                        emergency = false
                    ),
                    MenuChangeItemInput(
                        menuDayId = day.id,
                        day = dateIso,
                        mealType = "dinner",
                        newValue = newDinner,
                        reason = "Actualizado por Nutricionista",
                        emergency = false
                    )
                )

                vm.proposeChanges(items) {
                    loadMenuForCurrentMonth()
                    dialog.dismiss()
                }
            }

        dialog.show()
    }

    private fun performLogout() {
        // TODO: Implementar logout real
        Toast.makeText(requireContext(), "Cerrar sesión", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentView = null
        selectedDay = null
    }
}
