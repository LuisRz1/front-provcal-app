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
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val monthIndex = calendar.get(Calendar.MONTH) // 0-based

        val tmpCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstWeekday = tmpCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday ... 7 = Saturday

        // Mapeo para cabecera: Lun, Mar, Mié, Jue, Vie, Sab, Dom
        val offset = when (firstWeekday) {
            Calendar.MONDAY    -> 0
            Calendar.TUESDAY   -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY  -> 3
            Calendar.FRIDAY    -> 4
            Calendar.SATURDAY  -> 5
            Calendar.SUNDAY    -> 6
            else               -> 0
        }

        // espacios vacíos antes del 1
        for (i in 0 until offset) {
            val spacer = View(requireContext())
            val lp = LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
            )
            lp.marginEnd = dpToPx(8)
            spacer.layoutParams = lp
            row.addView(spacer)
        }

        tmpCal.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthNumber = monthIndex + 1

        for (dayNum in 1..daysInMonth) {
            val dateIso = String.format(Locale.US, "%04d-%02d-%02d", year, monthNumber, dayNum)

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
                        isSelected -> Color.parseColor("#18A558")
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
                            hasMenu   -> Color.parseColor("#202124")
                            else      -> Color.parseColor("#BDBDBD")
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

    // ===== CARD DEL MENÚ DEL DÍA =====
    private fun renderMenuDayCard() {
        val day = selectedDay ?: return
        val container = currentView?.findViewById<LinearLayout>(R.id.containerMenuDay) ?: return

        container.removeAllViews()

        val cardView = layoutInflater.inflate(R.layout.layout_menu_day_card, container, false)

        val meals = day.meals ?: emptyList()

        // Labels principales de las dos filas grandes
        val tvMainLabel1 = cardView.findViewById<TextView>(R.id.tvMainLabel1)
        val tvMainLabel2 = cardView.findViewById<TextView>(R.id.tvMainLabel2)

        // Views principales
        val tvBebida = cardView.findViewById<TextView>(R.id.tvBebida)
        val tvBebidaCal = cardView.findViewById<TextView>(R.id.tvBebidaCalorias)
        val tvPlato = cardView.findViewById<TextView>(R.id.tvPlato)
        val tvPlatoCal = cardView.findViewById<TextView>(R.id.tvPlatoCalorias)
        val tvTotal = cardView.findViewById<TextView>(R.id.tvTotal)

        val rowGuarn1 = cardView.findViewById<LinearLayout>(R.id.rowGuarnicion1)
        val rowGuarn2 = cardView.findViewById<LinearLayout>(R.id.rowGuarnicion2)
        val rowPan = cardView.findViewById<LinearLayout>(R.id.rowPan)
        val rowSandwich1 = cardView.findViewById<LinearLayout>(R.id.rowSandwich1)
        val rowSandwich2 = cardView.findViewById<LinearLayout>(R.id.rowSandwich2)

        val tvGuarnLabel1 = cardView.findViewById<TextView>(R.id.tvGuarnLabel1)
        val tvGuarn1 = cardView.findViewById<TextView>(R.id.tvGuarn1)
        val tvGuarnCal1 = cardView.findViewById<TextView>(R.id.tvGuarnCal1)

        val tvGuarnLabel2 = cardView.findViewById<TextView>(R.id.tvGuarnLabel2)
        val tvGuarn2 = cardView.findViewById<TextView>(R.id.tvGuarn2)
        val tvGuarnCal2 = cardView.findViewById<TextView>(R.id.tvGuarnCal2)

        val tvPanLabel = cardView.findViewById<TextView>(R.id.tvPanLabel)
        val tvPan = cardView.findViewById<TextView>(R.id.tvPan)
        val tvPanCalorias = cardView.findViewById<TextView>(R.id.tvPanCalorias)

        val tvSandwichLabel1 = cardView.findViewById<TextView>(R.id.tvSandwichLabel1)
        val tvSandwich1 = cardView.findViewById<TextView>(R.id.tvSandwich1)
        val tvSandwichCal1 = cardView.findViewById<TextView>(R.id.tvSandwichCal1)

        val tvSandwichLabel2 = cardView.findViewById<TextView>(R.id.tvSandwichLabel2)
        val tvSandwich2 = cardView.findViewById<TextView>(R.id.tvSandwich2)
        val tvSandwichCal2 = cardView.findViewById<TextView>(R.id.tvSandwichCal2)

        // Reset de filas opcionales
        rowGuarn1.visibility = View.GONE
        rowGuarn2.visibility = View.GONE
        rowPan.visibility = View.GONE
        rowSandwich2.visibility = View.GONE
        rowSandwich1.visibility = View.GONE

        // ===== Helpers =====
        fun normalizeKey(raw: String?): String {
            if (raw == null) return ""
            val upper = raw.trim().uppercase(Locale.ROOT)
            val noAccents = upper
                .replace('Á', 'A')
                .replace('É', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ú', 'U')
                .replace('Ü', 'U')
                .replace('Ñ', 'N')
            return noAccents.replace("\\s+".toRegex(), " ")
        }

        fun getComponentsFor(mealTypeName: String): List<MonthlyMenuQuery.Component> {
            val target = normalizeKey(mealTypeName)
            val meal = meals.firstOrNull {
                val mt = normalizeKey(it.mealType?.toString())
                mt == target
            }

            val list = meal?.components
                ?.filterNotNull()
                ?.sortedBy { it.order ?: Int.MAX_VALUE }
                ?: emptyList()

            Log.d(
                "MenuDebug",
                "getComponentsFor($mealTypeName) -> ${list.map { "${it.order}:${it.componentType}:${it.dishName}" }}"
            )

            return list
        }

        fun findComp(
            components: List<MonthlyMenuQuery.Component>,
            vararg types: String
        ): MonthlyMenuQuery.Component? {
            val candidates = types.map { normalizeKey(it) }.filter { it.isNotEmpty() }
            if (candidates.isEmpty()) return null

            return components.firstOrNull { comp ->
                val t = normalizeKey(comp.componentType)
                if (t.isEmpty()) return@firstOrNull false

                candidates.any { cand ->
                    t == cand || t.contains(cand)
                }
            }
        }

        // Fallback: primero busca por tipo, y si no encuentra, usa la posición (order / índice)
        fun compByTypeOrIndex(
            components: List<MonthlyMenuQuery.Component>,
            index: Int,
            vararg types: String
        ): MonthlyMenuQuery.Component? {
            val byType = findComp(components, *types)
            if (byType != null) return byType

            val sorted = components.sortedBy { it.order ?: Int.MAX_VALUE }
            return sorted.getOrNull(index)
        }

        fun setMainRow(textView: TextView, calView: TextView, comp: MonthlyMenuQuery.Component?) {
            if (comp == null || comp.dishName.isNullOrBlank()) {
                textView.text = "—"
                calView.text = "—"
            } else {
                textView.text = comp.dishName
                calView.text = comp.calories?.let { "$it Kcal" } ?: "—"
            }
        }

        var totalCalories = 0

        when (currentMeal) {
            // ===== DESAYUNO =====
            "breakfast" -> {
                tvMainLabel1.text = "Bebida Caliente"
                tvMainLabel2.text = "Plato Caliente"

                val components = getComponentsFor("BREAKFAST")
                totalCalories = components.sumOf { it.calories ?: 0 }

                val bebida = compByTypeOrIndex(
                    components,
                    0,
                    "BEBIDA CALIENTE",
                    "BEBIDA"
                )
                val platoCaliente = compByTypeOrIndex(
                    components,
                    1,
                    "PLATO CALIENTE",
                    "PLATO DE FONDO"
                )

                val guarn1 = compByTypeOrIndex(
                    components,
                    2,
                    "GUARNICION 1",
                    "GUARNICIÓN 1",
                    "GUARNICION"
                )
                val guarn2 = compByTypeOrIndex(
                    components,
                    3,
                    "GUARNICION 2",
                    "GUARNICIÓN 2"
                )

                val panComp = compByTypeOrIndex(
                    components,
                    4,
                    "PAN"
                )

                val sand1 = compByTypeOrIndex(
                    components,
                    5,
                    "SANDWICH 1",
                    "SÁNDWICH 1",
                    "SANDWICH"
                )
                val sand2 = compByTypeOrIndex(
                    components,
                    6,
                    "SANDWICH 2",
                    "SÁNDWICH 2"
                )

                // Bebida y plato caliente
                setMainRow(tvBebida, tvBebidaCal, bebida)
                setMainRow(tvPlato, tvPlatoCal, platoCaliente)

                // Guarniciones
                val hasG1 = guarn1?.dishName?.isNotBlank() == true
                val hasG2 = guarn2?.dishName?.isNotBlank() == true

                if (!hasG1 && !hasG2) {
                    rowGuarn1.visibility = View.GONE
                    rowGuarn2.visibility = View.GONE
                } else if (hasG1 && !hasG2) {
                    rowGuarn1.visibility = View.VISIBLE
                    tvGuarnLabel1.text = "Guarnición"
                    setMainRow(tvGuarn1, tvGuarnCal1, guarn1)
                    rowGuarn2.visibility = View.GONE
                } else if (!hasG1 && hasG2) {
                    rowGuarn1.visibility = View.VISIBLE
                    tvGuarnLabel1.text = "Guarnición"
                    setMainRow(tvGuarn1, tvGuarnCal1, guarn2)
                    rowGuarn2.visibility = View.GONE
                } else {
                    rowGuarn1.visibility = View.VISIBLE
                    rowGuarn2.visibility = View.VISIBLE
                    tvGuarnLabel1.text = "Guarnición 1"
                    tvGuarnLabel2.text = "Guarnición 2"
                    setMainRow(tvGuarn1, tvGuarnCal1, guarn1)
                    setMainRow(tvGuarn2, tvGuarnCal2, guarn2)
                }

                // Pan
                if (panComp == null || panComp.dishName.isNullOrBlank()) {
                    rowPan.visibility = View.GONE
                } else {
                    rowPan.visibility = View.VISIBLE
                    tvPanLabel.text = "Pan"
                    setMainRow(tvPan, tvPanCalorias, panComp)
                }

                // Sandwiches
                val hasS1 = sand1?.dishName?.isNotBlank() == true
                val hasS2 = sand2?.dishName?.isNotBlank() == true

                if (!hasS1 && !hasS2) {
                    rowSandwich1.visibility = View.GONE
                    rowSandwich2.visibility = View.GONE
                } else if (hasS1 && !hasS2) {
                    rowSandwich1.visibility = View.VISIBLE
                    tvSandwichLabel1.text = "Sandwich"
                    setMainRow(tvSandwich1, tvSandwichCal1, sand1)
                    rowSandwich2.visibility = View.GONE
                } else if (!hasS1 && hasS2) {
                    rowSandwich1.visibility = View.VISIBLE
                    tvSandwichLabel1.text = "Sandwich"
                    setMainRow(tvSandwich1, tvSandwichCal1, sand2)
                    rowSandwich2.visibility = View.GONE
                } else {
                    rowSandwich1.visibility = View.VISIBLE
                    rowSandwich2.visibility = View.VISIBLE
                    tvSandwichLabel1.text = "Sandwich 1"
                    tvSandwichLabel2.text = "Sandwich 2"
                    setMainRow(tvSandwich1, tvSandwichCal1, sand1)
                    setMainRow(tvSandwich2, tvSandwichCal2, sand2)
                }
            }

            // ===== ALMUERZO =====
            "lunch" -> {
                tvMainLabel1.text = "Entrada"
                tvMainLabel2.text = "Plato de fondo 1"

                val components = getComponentsFor("LUNCH")
                totalCalories = components.sumOf { it.calories ?: 0 }

                val entrada     = compByTypeOrIndex(components, 0, "ENTRADA")
                val sopa        = compByTypeOrIndex(components, 1, "SOPA")
                val platoFondo1 = compByTypeOrIndex(
                    components,
                    2,
                    "PLATO DE FONDO 1",
                    "PLATO DE FONDO"
                )
                val platoFondo2 = compByTypeOrIndex(
                    components,
                    3,
                    "PLATO DE FONDO 2"
                )
                val guarn1      = compByTypeOrIndex(
                    components,
                    4,
                    "GUARNICION 1",
                    "GUARNICIÓN 1"
                )
                val guarn2      = compByTypeOrIndex(
                    components,
                    5,
                    "GUARNICION 2",
                    "GUARNICIÓN 2"
                )
                val acompan     = compByTypeOrIndex(
                    components,
                    6,
                    "ACOMPANAMIENTO",
                    "ACOMPAÑAMIENTO"
                )
                val postre      = compByTypeOrIndex(components, 7, "POSTRE")
                val refresco    = compByTypeOrIndex(components, 8, "REFRESCO")

                // Entrada y PF1 en las filas principales
                setMainRow(tvBebida, tvBebidaCal, entrada)
                setMainRow(tvPlato, tvPlatoCal, platoFondo1)

                // Sopa en Guarnición 1
                if (sopa != null && !sopa.dishName.isNullOrBlank()) {
                    rowGuarn1.visibility = View.VISIBLE
                    tvGuarnLabel1.text = "Sopa"
                    setMainRow(tvGuarn1, tvGuarnCal1, sopa)
                } else {
                    rowGuarn1.visibility = View.GONE
                }

                // Guarniciones combinadas en Guarnición 2
                if ((guarn1 != null && !guarn1.dishName.isNullOrBlank()) ||
                    (guarn2 != null && !guarn2.dishName.isNullOrBlank())
                ) {
                    rowGuarn2.visibility = View.VISIBLE
                    tvGuarnLabel2.text = "Guarnición"

                    val textoGuarn = when {
                        guarn1 != null && !guarn1.dishName.isNullOrBlank() &&
                                guarn2 != null && !guarn2.dishName.isNullOrBlank() ->
                            "${guarn1.dishName} / ${guarn2.dishName}"
                        guarn1 != null && !guarn1.dishName.isNullOrBlank() ->
                            guarn1.dishName
                        else ->
                            guarn2?.dishName ?: "—"
                    }

                    tvGuarn2.text = textoGuarn
                    tvGuarnCal2.text = "—"
                } else {
                    rowGuarn2.visibility = View.GONE
                }

                // Acompañamiento en la fila de Pan
                if (acompan != null && !acompan.dishName.isNullOrBlank()) {
                    rowPan.visibility = View.VISIBLE
                    tvPanLabel.text = "Acompañamiento"
                    setMainRow(tvPan, tvPanCalorias, acompan)
                } else {
                    rowPan.visibility = View.GONE
                }

                // Postre en Sandwich 1
                if (postre != null && !postre.dishName.isNullOrBlank()) {
                    rowSandwich1.visibility = View.VISIBLE
                    tvSandwichLabel1.text = "Postre"
                    setMainRow(tvSandwich1, tvSandwichCal1, postre)
                } else {
                    rowSandwich1.visibility = View.GONE
                }

                // Refresco en Sandwich 2
                if (refresco != null && !refresco.dishName.isNullOrBlank()) {
                    rowSandwich2.visibility = View.VISIBLE
                    tvSandwichLabel2.text = "Refresco"
                    setMainRow(tvSandwich2, tvSandwichCal2, refresco)
                } else {
                    rowSandwich2.visibility = View.GONE
                }
            }

            // ===== CENA =====
            "dinner" -> {
                tvMainLabel1.text = "Plato de fondo 1"
                tvMainLabel2.text = "Plato de fondo 2"

                val components = getComponentsFor("DINNER")
                totalCalories = components.sumOf { it.calories ?: 0 }

                val platoFondo1 = compByTypeOrIndex(
                    components,
                    0,
                    "PLATO DE FONDO 1",
                    "PLATO DE FONDO"
                )
                val platoFondo2 = compByTypeOrIndex(
                    components,
                    1,
                    "PLATO DE FONDO 2"
                )
                val guarn1      = compByTypeOrIndex(
                    components,
                    2,
                    "GUARNICION 1",
                    "GUARNICIÓN 1"
                )
                val guarn2      = compByTypeOrIndex(
                    components,
                    3,
                    "GUARNICION 2",
                    "GUARNICIÓN 2"
                )
                val acompan     = compByTypeOrIndex(
                    components,
                    4,
                    "ACOMPANAMIENTO",
                    "ACOMPAÑAMIENTO"
                )
                val infusion    = compByTypeOrIndex(
                    components,
                    5,
                    "INFUSION",
                    "INFUSIÓN"
                )

                setMainRow(tvBebida, tvBebidaCal, platoFondo1)
                setMainRow(tvPlato, tvPlatoCal, platoFondo2)

                // Guarniciones
                if (guarn1 != null && !guarn1.dishName.isNullOrBlank()) {
                    rowGuarn1.visibility = View.VISIBLE
                    tvGuarnLabel1.text = "Guarnición 1"
                    setMainRow(tvGuarn1, tvGuarnCal1, guarn1)
                } else rowGuarn1.visibility = View.GONE

                if (guarn2 != null && !guarn2.dishName.isNullOrBlank()) {
                    rowGuarn2.visibility = View.VISIBLE
                    tvGuarnLabel2.text = "Guarnición 2"
                    setMainRow(tvGuarn2, tvGuarnCal2, guarn2)
                } else rowGuarn2.visibility = View.GONE

                // Acompañamiento
                if (acompan != null && !acompan.dishName.isNullOrBlank()) {
                    rowPan.visibility = View.VISIBLE
                    tvPanLabel.text = "Acompañamiento"
                    setMainRow(tvPan, tvPanCalorias, acompan)
                } else rowPan.visibility = View.GONE

                // Infusión
                if (infusion != null && !infusion.dishName.isNullOrBlank()) {
                    rowSandwich1.visibility = View.VISIBLE
                    tvSandwichLabel1.text = "Infusión"
                    setMainRow(tvSandwich1, tvSandwichCal1, infusion)
                } else rowSandwich1.visibility = View.GONE

                rowSandwich2.visibility = View.GONE
            }
        }

        tvTotal.text = if (totalCalories > 0) {
            "Total: $totalCalories Kcal"
        } else {
            "Total: —"
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
        val yearText = currentView
            ?.findViewById<AutoCompleteTextView>(R.id.acYear)
            ?.text
            ?.toString()

        val monthText = currentView
            ?.findViewById<AutoCompleteTextView>(R.id.acMonth)
            ?.text
            ?.toString()

        val y = yearText?.toIntOrNull() ?: calendar.get(Calendar.YEAR)

        val months = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val monthIndexFromUi = months.indexOf(monthText)
        val m = if (monthIndexFromUi >= 0) monthIndexFromUi + 1 else calendar.get(Calendar.MONTH) + 1

        val fileName = getFileName(uri) ?: "menu_${y}_${m}.xlsx"

        val base64 = readAsBase64(uri) ?: run {
            Toast.makeText(
                requireContext(),
                "No se pudo leer el archivo",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Importar menú")
            .setMessage("Se importará el archivo:\n\n$fileName\n\npara $m/$y.")
            .setPositiveButton("Importar") { _, _ ->
                vm.uploadMenu(y, m, fileName, base64, overwrite = false) { conflict ->
                    if (conflict) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Sobrescribir menú")
                            .setMessage("Ya existe un menú para este mes. ¿Deseas reemplazarlo?")
                            .setPositiveButton("Sí, reemplazar") { _, _ ->
                                vm.uploadMenu(y, m, fileName, base64, overwrite = true) {
                                    calendar.set(Calendar.YEAR, y)
                                    calendar.set(Calendar.MONTH, m - 1)
                                    showListView()
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        calendar.set(Calendar.YEAR, y)
                        calendar.set(Calendar.MONTH, m - 1)
                        showListView()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
