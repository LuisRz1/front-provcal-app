package com.sanna.provcalapp.utils

import com.sanna.provcalapp.MonthlyMenuQuery
import com.sanna.provcalapp.ProposeMenuChangeMutation
import com.sanna.provcalapp.UploadMonthlyMenuMutation
import com.sanna.provcalapp.ConfirmOverwriteMonthlyMenuMutation
import com.sanna.provcalapp.type.MenuChangeItemInput

/**
 * Test data fixtures for Menu feature tests.
 * Contains mock GraphQL responses and input data for testing MenuRepository and MenuViewModel.
 */
object MenuTestData {

    // Mock Menu Day Data
    val mockDay1 = MonthlyMenuQuery.Day(
        id = "1",
        date = "2025-01-15",
        breakfast = "Pan con palta|Jugo de naranja",
        lunch = "Arroz con pollo|Ensalada verde",
        dinner = "Sopa de verduras|Pan integral",
        isHoliday = false
    )

    val mockDay2 = MonthlyMenuQuery.Day(
        id = "2",
        date = "2025-01-16",
        breakfast = "Avena con frutas|Café",
        lunch = "Pescado al horno|Puré de papa",
        dinner = "Ensalada César|Pan",
        isHoliday = false
    )

    val mockHolidayDay = MonthlyMenuQuery.Day(
        id = "3",
        date = "2025-01-17",
        breakfast = "",
        lunch = "",
        dinner = "",
        isHoliday = true
    )

    // Mock Menu with multiple days
    val mockMenu = MonthlyMenuQuery.Menu(
        year = 2025,
        month = 1,
        days = listOf(mockDay1, mockDay2, mockHolidayDay)
    )

    // Mock Menu with single day
    val mockMenuSingleDay = MonthlyMenuQuery.Menu(
        year = 2025,
        month = 2,
        days = listOf(mockDay1)
    )

    // Mock empty menu (no days)
    val mockEmptyMenu = MonthlyMenuQuery.Menu(
        year = 2025,
        month = 3,
        days = emptyList()
    )

    // Upload Responses
    val mockUploadSuccessResponse = UploadMonthlyMenuMutation.UploadMonthlyMenu(
        status = "ok",
        message = "Menú cargado exitosamente"
    )

    val mockUploadSuccessEmptyMessage = UploadMonthlyMenuMutation.UploadMonthlyMenu(
        status = "ok",
        message = ""
    )

    val mockUploadConflictResponse = UploadMonthlyMenuMutation.UploadMonthlyMenu(
        status = "conflict",
        message = "Ya existe un menú para este mes"
    )

    val mockUploadErrorResponse = UploadMonthlyMenuMutation.UploadMonthlyMenu(
        status = "error",
        message = "Error al procesar el archivo"
    )

    val mockUploadUnknownStatusResponse = UploadMonthlyMenuMutation.UploadMonthlyMenu(
        status = "unknown",
        message = "Estado desconocido"
    )

    // Confirm Overwrite Responses
    val mockConfirmOverwriteSuccessResponse = ConfirmOverwriteMonthlyMenuMutation.ConfirmOverwriteMenu(
        status = "ok",
        message = "Menú reemplazado exitosamente"
    )

    val mockConfirmOverwriteErrorResponse = ConfirmOverwriteMonthlyMenuMutation.ConfirmOverwriteMenu(
        status = "error",
        message = "Error al reemplazar menú"
    )

    // Propose Menu Change Responses
    val mockProposeChangeResponse = ProposeMenuChangeMutation.ProposeMenuChange(
        id = "123",
        status = "pending"
    )

    val mockProposeChangeResponse2 = ProposeMenuChangeMutation.ProposeMenuChange(
        id = "124",
        status = "pending"
    )

    val mockProposeChangeResponse3 = ProposeMenuChangeMutation.ProposeMenuChange(
        id = "125",
        status = "pending"
    )

    // Menu Change Input Items
    val mockMenuChangeItem = MenuChangeItemInput(
        menuDayId = "1",
        day = "2025-01-15",
        mealType = "lunch",
        newValue = "Ceviche peruano",
        reason = "Cambio solicitado por nutricionista",
        emergency = false
    )

    val mockMenuChangeItemEmergency = MenuChangeItemInput(
        menuDayId = "1",
        day = "2025-01-15",
        mealType = "breakfast",
        newValue = "Pan con huevo",
        reason = "Cambio de último momento",
        emergency = true
    )

    val mockMenuChangeItem2 = MenuChangeItemInput(
        menuDayId = "2",
        day = "2025-01-16",
        mealType = "dinner",
        newValue = "Sopa de pollo",
        reason = "Preferencia de comensales",
        emergency = false
    )

    // Base64 encoded mock file data
    const val mockFileBase64 = "UEsDBBQACAgIAAAAIQAAAAAAAAAAAAAAAA=="  // Mock Excel file

    // Common test parameters
    const val testYear = 2025
    const val testMonth = 1
    const val testFilename = "menu_enero_2025.xlsx"

    // Error messages
    const val networkErrorMessage = "Error de conexión: Network timeout"
    const val graphqlErrorMessage = "GraphQL Error: Invalid query"
    const val noDataErrorMessage = "Sin datos de menú para la fecha especificada"
}
