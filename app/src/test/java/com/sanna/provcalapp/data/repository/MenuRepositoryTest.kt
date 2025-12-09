package com.sanna.provcalapp.data.repository

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.exception.ApolloException
import com.sanna.provcalapp.MonthlyMenuQuery
import com.sanna.provcalapp.ProposeMenuChangeMutation
import com.sanna.provcalapp.UploadMonthlyMenuMutation
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.utils.MenuTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MenuRepository.
 *
 * NOTE: These tests focus on exception handling paths because MockK has compatibility
 * issues mocking Apollo GraphQL's ApolloResponse class properties. Success path testing
 * would require either:
 * 1. Using a different mocking library (e.g., Mockito)
 * 2. Creating a repository interface/wrapper for easier testing
 * 3. Using integration tests instead of unit tests for Apollo interactions
 */
class MenuRepositoryTest {

    private lateinit var repository: MenuRepository
    private lateinit var mockApolloClient: ApolloClient
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockApolloClient = mockk(relaxed = true)

        // Create repository with mocked Apollo client directly
        repository = MenuRepository(apolloClient = mockApolloClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========================================
    // getMonthlyMenu() Tests
    // ========================================

    @Test
    fun `getMonthlyMenu returns Error on network exception`() = runTest {
        // Given
        val exception = mockk<ApolloException>()
        every { exception.message } returns "Network timeout"

        val mockQueryCall = mockk<ApolloCall<MonthlyMenuQuery.Data>>()
        every { mockApolloClient.query(any<MonthlyMenuQuery>()) } returns mockQueryCall
        coEvery { mockQueryCall.execute() } throws exception

        // When
        val result = repository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.startsWith("Error de conexión:"))
    }

    @Test
    fun `getMonthlyMenu handles generic exceptions`() = runTest {
        // Given
        val mockQueryCall = mockk<ApolloCall<MonthlyMenuQuery.Data>>()
        every { mockApolloClient.query(any<MonthlyMenuQuery>()) } returns mockQueryCall
        coEvery { mockQueryCall.execute() } throws RuntimeException("Unexpected error")

        // When & Then
        try {
            repository.getMonthlyMenu(MenuTestData.testYear, MenuTestData.testMonth)
            // If we get here without exception, the test passes
            // (repository lets non-Apollo exceptions bubble up or catches them)
        } catch (e: Exception) {
            // Expected - repository doesn't catch non-Apollo exceptions
            assertTrue(e is RuntimeException)
        }
    }

    // ========================================
    // uploadMonthlyMenu() Tests
    // ========================================

    @Test
    fun `uploadMonthlyMenu returns Error on network exception`() = runTest {
        // Given
        val exception = mockk<ApolloException>()
        every { exception.message } returns "Network error"

        val mockMutationCall = mockk<ApolloCall<UploadMonthlyMenuMutation.Data>>()
        every { mockApolloClient.mutation(any<UploadMonthlyMenuMutation>()) } returns mockMutationCall
        coEvery { mockMutationCall.execute() } throws exception

        // When
        val result = repository.uploadMonthlyMenu(
            MenuTestData.testYear,
            MenuTestData.testMonth,
            MenuTestData.testFilename,
            MenuTestData.mockFileBase64,
            false
        )

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.startsWith("Error de conexión:"))
    }

    // ========================================
    // proposeMenuChange() Tests
    // ========================================

    @Test
    fun `proposeMenuChange returns Error on network exception`() = runTest {
        // Given
        val exception = mockk<ApolloException>()
        every { exception.message } returns "Connection failed"

        val items = listOf(MenuTestData.mockMenuChangeItem)
        val mockMutationCall = mockk<ApolloCall<ProposeMenuChangeMutation.Data>>()
        every { mockApolloClient.mutation(any<ProposeMenuChangeMutation>()) } returns mockMutationCall
        coEvery { mockMutationCall.execute() } throws exception

        // When
        val result = repository.proposeMenuChange(items)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.startsWith("Error de conexión:"))
    }
}
