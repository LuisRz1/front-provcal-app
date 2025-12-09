package com.sanna.provcalapp.ui.menu

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.repository.MenuRepository
import com.sanna.provcalapp.type.MenuChangeItemInput
import com.sanna.provcalapp.utils.MainCoroutineRule
import com.sanna.provcalapp.utils.MenuTestData
import com.sanna.provcalapp.utils.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MenuViewModelTest {

    // Rules for LiveData and Coroutines testing
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: MenuViewModel
    private lateinit var mockRepository: MenuRepository
    private lateinit var mockApplication: Application

    @Before
    fun setup() {
        mockApplication = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)

        // Create ViewModel with mocked repository
        viewModel = MenuViewModel(mockApplication)
        // Inject mocked repository via reflection since it's private
        val repositoryField = MenuViewModel::class.java.getDeclaredField("repo")
        repositoryField.isAccessible = true
        repositoryField.set(viewModel, mockRepository)
    }

    // ========================================
    // loadMenu() Tests
    // ========================================

    @Test
    fun `loadMenu updates menu LiveData on success`() = runTest {
        // Given
        coEvery {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        } returns Result.Success(MenuTestData.mockMenu)

        // When
        viewModel.loadMenu(MenuTestData.testYear, MenuTestData.testMonth)

        // Then
        assertEquals(MenuTestData.mockMenu, viewModel.menu.getOrAwaitValue())
        coVerify(exactly = 1) {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        }
    }

    @Test
    fun `loadMenu sets message to null on success`() = runTest {
        // Given
        coEvery {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        } returns Result.Success(MenuTestData.mockMenu)

        // When
        viewModel.loadMenu(MenuTestData.testYear, MenuTestData.testMonth)

        // Then
        assertNull(viewModel.message.getOrAwaitValue())
    }

    @Test
    fun `loadMenu sets loading to false after success`() = runTest {
        // Given
        coEvery {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        } returns Result.Success(MenuTestData.mockMenu)

        // When
        viewModel.loadMenu(MenuTestData.testYear, MenuTestData.testMonth)

        // Then
        assertFalse(viewModel.loading.getOrAwaitValue())
    }

    @Test
    fun `loadMenu updates message LiveData on error`() = runTest {
        // Given
        val errorMessage = "Error loading menu"
        coEvery {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        } returns Result.Error(errorMessage)

        // When
        viewModel.loadMenu(MenuTestData.testYear, MenuTestData.testMonth)

        // Then
        assertEquals(errorMessage, viewModel.message.getOrAwaitValue())
    }

    @Test
    fun `loadMenu does not update menu LiveData on error`() = runTest {
        // Given
        val initialMenu = viewModel.menu.value
        coEvery {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        } returns Result.Error("Error loading menu")

        // When
        viewModel.loadMenu(MenuTestData.testYear, MenuTestData.testMonth)

        // Then
        // Menu should remain unchanged (null or previous value)
        assertEquals(initialMenu, viewModel.menu.value)
    }

    @Test
    fun `loadMenu sets loading to false after error`() = runTest {
        // Given
        coEvery {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        } returns Result.Error("Error loading menu")

        // When
        viewModel.loadMenu(MenuTestData.testYear, MenuTestData.testMonth)

        // Then
        assertFalse(viewModel.loading.getOrAwaitValue())
    }

    @Test
    fun `loadMenu handles empty menu successfully`() = runTest {
        // Given
        coEvery {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, 3)
        } returns Result.Success(MenuTestData.mockEmptyMenu)

        // When
        viewModel.loadMenu(MenuTestData.testYear, 3)

        // Then
        assertEquals(MenuTestData.mockEmptyMenu, viewModel.menu.getOrAwaitValue())
        assertTrue(viewModel.menu.getOrAwaitValue()?.days?.isEmpty() == true)
    }

    // ========================================
    // uploadMenu() Tests
    // ========================================

    @Test
    fun `uploadMenu updates message LiveData on success`() = runTest {
        // Given
        val successMessage = "Menú cargado exitosamente"
        coEvery {
            mockRepository.uploadMonthlyMenu(any(), any(), any(), any(), any())
        } returns Result.Success(successMessage)

        coEvery {
            mockRepository.getMonthlyMenu(any(), any())
        } returns Result.Success(MenuTestData.mockMenu)

        // When
        var conflictCalled = false
        viewModel.uploadMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        ) { conflictCalled = true }

        // Then
        // After successful upload, loadMenu() is called which sets message to null on success
        assertNull(viewModel.message.getOrAwaitValue())
        assertFalse(conflictCalled)
    }

    @Test
    fun `uploadMenu calls loadMenu to refresh on success`() = runTest {
        // Given
        coEvery {
            mockRepository.uploadMonthlyMenu(any(), any(), any(), any(), any())
        } returns Result.Success("Success")

        coEvery {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        } returns Result.Success(MenuTestData.mockMenu)

        // When
        viewModel.uploadMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        ) { }

        // Then
        coVerify(exactly = 1) {
            mockRepository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
        }
    }

    @Test
    fun `uploadMenu sets loading to false after success`() = runTest {
        // Given
        coEvery {
            mockRepository.uploadMonthlyMenu(any(), any(), any(), any(), any())
        } returns Result.Success("Success")

        coEvery {
            mockRepository.getMonthlyMenu(any(), any())
        } returns Result.Success(MenuTestData.mockMenu)

        // When
        viewModel.uploadMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        ) { }

        // Then
        assertFalse(viewModel.loading.getOrAwaitValue())
    }

    @Test
    fun `uploadMenu invokes onConflict callback when error starts with conflict prefix`() = runTest {
        // Given
        coEvery {
            mockRepository.uploadMonthlyMenu(any(), any(), any(), any(), any())
        } returns Result.Error("conflict: Ya existe un menú")

        // When
        var conflictCalled = false
        viewModel.uploadMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        ) { conflictCalled = true }

        // Then
        assertTrue(conflictCalled)
    }

    @Test
    fun `uploadMenu does not update message when conflict is detected`() = runTest {
        // Given
        val initialMessage = viewModel.message.value
        coEvery {
            mockRepository.uploadMonthlyMenu(any(), any(), any(), any(), any())
        } returns Result.Error("conflict: Ya existe un menú")

        // When
        viewModel.uploadMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        ) { }

        // Then
        // Message should remain unchanged when conflict is handled by callback
        assertEquals(initialMessage, viewModel.message.value)
    }

    @Test
    fun `uploadMenu updates message LiveData on non-conflict error`() = runTest {
        // Given
        val errorMessage = "Error uploading file"
        coEvery {
            mockRepository.uploadMonthlyMenu(any(), any(), any(), any(), any())
        } returns Result.Error(errorMessage)

        // When
        var conflictCalled = false
        viewModel.uploadMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        ) { conflictCalled = true }

        // Then
        assertEquals(errorMessage, viewModel.message.getOrAwaitValue())
        assertFalse(conflictCalled)
    }

    @Test
    fun `uploadMenu does not call loadMenu on error`() = runTest {
        // Given
        coEvery {
            mockRepository.uploadMonthlyMenu(any(), any(), any(), any(), any())
        } returns Result.Error("Upload failed")

        // When
        viewModel.uploadMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        ) { }

        // Then
        coVerify(exactly = 0) {
            mockRepository.getMonthlyMenu(any(), any())
        }
    }

    @Test
    fun `uploadMenu sets loading to false after error`() = runTest {
        // Given
        coEvery {
            mockRepository.uploadMonthlyMenu(any(), any(), any(), any(), any())
        } returns Result.Error("Upload failed")

        // When
        viewModel.uploadMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        ) { }

        // Then
        assertFalse(viewModel.loading.getOrAwaitValue())
    }

    // ========================================
    // proposeChanges() Tests
    // ========================================

    @Test
    fun `proposeChanges updates message with success count`() = runTest {
        // Given
        val changeCount = 5
        val items = listOf(MenuTestData.mockMenuChangeItem)
        coEvery {
            mockRepository.proposeMenuChange(items)
        } returns Result.Success(changeCount)

        // When
        var doneCalled = false
        var doneCount = 0
        viewModel.proposeChanges(items) { count ->
            doneCalled = true
            doneCount = count
        }

        // Then
        assertEquals("Cambios propuestos: 5", viewModel.message.getOrAwaitValue())
        assertTrue(doneCalled)
        assertEquals(changeCount, doneCount)
    }

    @Test
    fun `proposeChanges invokes onDone callback with correct count`() = runTest {
        // Given
        val changeCount = 3
        val items = listOf(
            MenuTestData.mockMenuChangeItem,
            MenuTestData.mockMenuChangeItem2,
            MenuTestData.mockMenuChangeItemEmergency
        )
        coEvery {
            mockRepository.proposeMenuChange(items)
        } returns Result.Success(changeCount)

        // When
        var doneCalled = false
        var receivedCount = 0
        viewModel.proposeChanges(items) { count ->
            doneCalled = true
            receivedCount = count
        }

        // Then
        assertTrue(doneCalled)
        assertEquals(changeCount, receivedCount)
    }

    @Test
    fun `proposeChanges sets loading to false after success`() = runTest {
        // Given
        val items = listOf(MenuTestData.mockMenuChangeItem)
        coEvery {
            mockRepository.proposeMenuChange(items)
        } returns Result.Success(2)

        // When
        viewModel.proposeChanges(items) { }

        // Then
        assertFalse(viewModel.loading.getOrAwaitValue())
    }

    @Test
    fun `proposeChanges handles zero changes successfully`() = runTest {
        // Given
        val items = emptyList<MenuChangeItemInput>()
        coEvery {
            mockRepository.proposeMenuChange(items)
        } returns Result.Success(0)

        // When
        var receivedCount = -1
        viewModel.proposeChanges(items) { count ->
            receivedCount = count
        }

        // Then
        assertEquals("Cambios propuestos: 0", viewModel.message.getOrAwaitValue())
        assertEquals(0, receivedCount)
    }

    @Test
    fun `proposeChanges updates message LiveData on error`() = runTest {
        // Given
        val errorMessage = "Error proponiendo cambios"
        val items = listOf(MenuTestData.mockMenuChangeItem)
        coEvery {
            mockRepository.proposeMenuChange(items)
        } returns Result.Error(errorMessage)

        // When
        var doneCalled = false
        viewModel.proposeChanges(items) { doneCalled = true }

        // Then
        assertEquals(errorMessage, viewModel.message.getOrAwaitValue())
        assertFalse(doneCalled)
    }

    @Test
    fun `proposeChanges does not invoke onDone callback on error`() = runTest {
        // Given
        val items = listOf(MenuTestData.mockMenuChangeItem)
        coEvery {
            mockRepository.proposeMenuChange(items)
        } returns Result.Error("Failed to propose changes")

        // When
        var doneCalled = false
        viewModel.proposeChanges(items) { doneCalled = true }

        // Then
        assertFalse(doneCalled)
    }

    @Test
    fun `proposeChanges sets loading to false after error`() = runTest {
        // Given
        val items = listOf(MenuTestData.mockMenuChangeItem)
        coEvery {
            mockRepository.proposeMenuChange(items)
        } returns Result.Error("Error")

        // When
        viewModel.proposeChanges(items) { }

        // Then
        assertFalse(viewModel.loading.getOrAwaitValue())
    }
}
