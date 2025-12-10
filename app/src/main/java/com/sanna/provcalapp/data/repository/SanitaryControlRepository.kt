package com.sanna.provcalapp.data.repository

import android.content.Context
import com.apollographql.apollo.exception.ApolloException
import com.sanna.provcalapp.GetSanitaryCompaniesQuery
import com.sanna.provcalapp.GetSanitaryIncidentTypesByPolicyQuery
import com.sanna.provcalapp.GetSanitaryPoliciesQuery
import com.sanna.provcalapp.GetSanitaryPolicyHistoryQuery
import com.sanna.provcalapp.RegisterSanitaryReviewMutation
import com.sanna.provcalapp.data.local.SanitaryControlManager
import com.sanna.provcalapp.data.models.*
import com.sanna.provcalapp.data.remote.ApolloClientProvider
import com.sanna.provcalapp.type.RegisterSanitaryReviewInput as GqlRegisterInput
import com.sanna.provcalapp.utils.NetworkMonitor
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for Sanitary Control (Health Policy) operations
 * Uses GraphQL backend via Apollo Client
 *
 * Constructor supports dependency injection for testing
 */
class SanitaryControlRepository(
    private val context: Context
) {

    private val localManager = SanitaryControlManager(context)
    private val apolloClient = ApolloClientProvider.getInstance(context)
    private val networkMonitor = NetworkMonitor(context)

    // Cache for companies and incident types to avoid repeated queries
    private var companiesCache: List<Company> = emptyList()
    private var incidentTypesCache: MutableMap<String, List<IncidentType>> = mutableMapOf()

    companion object {
        // Date format constants
        private const val ISO_DATE_FORMAT = "yyyy-MM-dd"
        private const val DISPLAY_DATE_FORMAT = "dd/MM/yyyy"
        private const val DISPLAY_DATE_SHORT_FORMAT = "dd/MM/yy"
    }

    /**
     * Get all active health policies from backend
     */
    suspend fun getPolicies(): Result<List<HealthPolicy>> {
        return try {
            // Check network availability first
            if (!networkMonitor.isNetworkAvailable()) {
                return Result.Error("Sin conexión a internet. Por favor, verifica tu conexión.")
            }

            val response = apolloClient.query(GetSanitaryPoliciesQuery()).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error al obtener políticas")
            } else {
                val data = response.data?.sanitaryPolicies
                if (data?.success == true) {
                    val policies = data.policies.map { policy ->
                        HealthPolicy(
                            id = policy.id,
                            name = policy.name,
                            description = policy.description ?: "",
                            type = mapPolicyType(policy.name),
                            isActive = policy.isActive
                        )
                    }
                    localManager.saveLastSyncTimestamp()
                    Result.Success(policies)
                } else {
                    Result.Error(data?.message ?: "Error desconocido al obtener políticas")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Get revision history for a specific policy from backend
     * @param policyId Policy identifier
     * @param filterMonths Number of months to filter (6, 12, 24)
     */
    suspend fun getRevisionHistory(
        policyId: String,
        filterMonths: Int? = null
    ): Result<List<RevisionHistoryItem>> {
        return try {
            // Check network availability first
            if (!networkMonitor.isNetworkAvailable()) {
                return Result.Error("Sin conexión a internet. Por favor, verifica tu conexión.")
            }

            // Default to 24 months if not specified
            val monthsBack = filterMonths ?: 24

            // Ensure companies and incident types are loaded for mapping
            ensureCompaniesLoaded()
            ensureIncidentTypesLoaded(policyId)

            val response = apolloClient.query(
                GetSanitaryPolicyHistoryQuery(policyId = policyId, monthsBack = monthsBack)
            ).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error al obtener historial")
            } else {
                val data = response.data?.sanitaryPolicyHistory
                if (data?.success == true) {
                    val history = data.history.map { review ->
                        mapReviewToHistoryItem(review, policyId)
                    }
                    Result.Success(history)
                } else {
                    Result.Error(data?.message ?: "Error desconocido al obtener historial")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Get next scheduled revision date for a policy from backend
     * Backend calculates this as last review + 30 days
     */
    suspend fun getNextRevisionDate(policyId: String): Result<String?> {
        return try {
            // Check network availability first
            if (!networkMonitor.isNetworkAvailable()) {
                return Result.Error("Sin conexión a internet. Por favor, verifica tu conexión.")
            }

            // Use the policy history query to get the next revision date
            val response = apolloClient.query(
                GetSanitaryPolicyHistoryQuery(policyId = policyId, monthsBack = 24)
            ).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error al obtener próxima revisión")
            } else {
                val data = response.data?.sanitaryPolicyHistory
                if (data?.success == true) {
                    val nextDate = data.nextReviewDate?.toString()?.let { formatDateFromISO(it) }
                    Result.Success(nextDate)
                } else {
                    Result.Error(data?.message ?: "Error desconocido al obtener próxima revisión")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Schedule/confirm a revision
     * NOTE: Backend auto-calculates next date, so this just returns success for UI flow
     */
    suspend fun scheduleRevision(policyId: String, date: String): Result<Boolean> {
        return try {
            // Backend doesn't support scheduling - it auto-calculates next date
            // This method kept for UI flow consistency
            localManager.saveNextRevisionDate(policyId, date)
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Error al programar revisión: ${e.message}", e)
        }
    }

    /**
     * Submit a new revision to backend
     */
    suspend fun submitRevision(input: SubmitRevisionInput): Result<SubmitRevisionResponse> {
        return try {
            // Check network availability first
            if (!networkMonitor.isNetworkAvailable()) {
                return Result.Error("Sin conexión a internet. Por favor, verifica tu conexión.")
            }

            // Validate: if Inconforme, require incidentTypeId and companyId
            if (!input.isConforme) {
                if (input.incidentTypeId == null || input.companyId == null) {
                    return Result.Error("Para revisión Inconforme, debe seleccionar tipo de incidencia y empresa")
                }
            }

            // Convert date format to ISO
            val isoDate = convertToISODate(input.date)

            val gqlInput = GqlRegisterInput(
                policyId = input.policyId,
                date = isoDate,
                isConform = input.isConforme,
                observation = com.apollographql.apollo.api.Optional.presentIfNotNull(input.observation),
                incidentTypeId = com.apollographql.apollo.api.Optional.presentIfNotNull(input.incidentTypeId),
                companyId = com.apollographql.apollo.api.Optional.presentIfNotNull(input.companyId)
            )

            val response = apolloClient.mutation(RegisterSanitaryReviewMutation(gqlInput)).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error al registrar revisión")
            } else {
                val data = response.data?.registerSanitaryReview
                if (data?.success == true && data.review != null) {
                    localManager.saveLastRevisionDate(input.policyId, input.date)
                    Result.Success(
                        SubmitRevisionResponse(
                            success = true,
                            message = data.message,
                            revisionId = data.review.id,
                            nextRevisionDate = null // Will be fetched from history query
                        )
                    )
                } else {
                    Result.Error(data?.message ?: "Error al registrar revisión")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Get specialized companies from backend
     * @param policyType Optional policy type to filter companies by name matching
     */
    suspend fun getCompanies(policyType: PolicyType? = null): Result<List<Company>> {
        return try {
            // Load from cache if already loaded
            if (companiesCache.isEmpty()) {
                val response = apolloClient.query(GetSanitaryCompaniesQuery()).execute()

                if (response.hasErrors()) {
                    return Result.Error(response.errors?.firstOrNull()?.message ?: "Error al obtener empresas")
                }

                val allCompanies = response.data?.sanitaryCompanies?.map { company ->
                    Company(
                        id = company.id,
                        socialReason = company.businessName,
                        ruc = company.ruc,
                        phone = company.phone ?: "",
                        email = company.email ?: "",
                        serviceType = "" // Backend doesn't provide this
                    )
                } ?: emptyList()

                companiesCache = allCompanies
            }

            // Filter by policy type if specified
            val filtered = if (policyType != null) {
                filterCompaniesByPolicyType(companiesCache, policyType)
            } else {
                companiesCache
            }

            Result.Success(filtered)
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}", e)
        }
    }

    /**
     * Get incident types for a specific policy from backend
     */
    suspend fun getIncidentTypes(policyId: String): Result<List<IncidentType>> {
        return try {
            // Load from cache if already loaded for this policy
            if (!incidentTypesCache.containsKey(policyId)) {
                val response = apolloClient.query(GetSanitaryIncidentTypesByPolicyQuery(policyId)).execute()

                if (response.hasErrors()) {
                    return Result.Error(response.errors?.firstOrNull()?.message ?: "Error al obtener tipos de incidencia")
                }

                val types = response.data?.sanitaryIncidentTypesByPolicy?.map { type ->
                    IncidentType(
                        id = type.id,
                        policyId = type.policyId,
                        name = type.name,
                        description = type.description
                    )
                } ?: emptyList()

                incidentTypesCache[policyId] = types
            }

            Result.Success(incidentTypesCache[policyId] ?: emptyList())
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}", e)
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Ensure companies are loaded into cache
     */
    private suspend fun ensureCompaniesLoaded() {
        if (companiesCache.isEmpty()) {
            getCompanies() // This will load into cache
        }
    }

    /**
     * Ensure incident types are loaded into cache for a policy
     */
    private suspend fun ensureIncidentTypesLoaded(policyId: String) {
        if (!incidentTypesCache.containsKey(policyId)) {
            getIncidentTypes(policyId) // This will load into cache
        }
    }

    /**
     * Map backend review to frontend history item
     */
    private fun mapReviewToHistoryItem(
        review: GetSanitaryPolicyHistoryQuery.History,
        policyId: String
    ): RevisionHistoryItem {
        val company = review.companyId?.let { id ->
            companiesCache.find { it.id == id }
        }

        val incidentType = review.incidentTypeId?.let { id ->
            incidentTypesCache[policyId]?.find { it.id == id }?.name
        }

        return RevisionHistoryItem(
            id = review.id,
            date = formatDateToDisplayShort(review.date.toString()),
            monthLabel = getMonthLabel(review.date.toString()),
            isConforme = review.isConform,
            observation = review.observation,
            incidentType = incidentType,
            company = company,
            hasIncident = !review.isConform
        )
    }

    /**
     * Map policy name to PolicyType enum
     */
    private fun mapPolicyType(name: String): PolicyType = when {
        name.contains("Plagas", ignoreCase = true) -> PolicyType.PEST_CONTROL
        name.contains("Residuos", ignoreCase = true) -> PolicyType.WASTE_MANAGEMENT
        name.contains("Comedor", ignoreCase = true) || name.contains("Saneamiento", ignoreCase = true) -> PolicyType.DINING_SANITATION
        else -> PolicyType.PEST_CONTROL
    }

    /**
     * Filter companies by policy type using name matching
     */
    private fun filterCompaniesByPolicyType(companies: List<Company>, policyType: PolicyType): List<Company> {
        val keyword = when (policyType) {
            PolicyType.PEST_CONTROL -> "fumigaci|plagas"
            PolicyType.WASTE_MANAGEMENT -> "residuos|ecogesti"
            PolicyType.DINING_SANITATION -> "limpieza|sanitari"
        }
        return companies.filter { it.socialReason.contains(keyword.toRegex(RegexOption.IGNORE_CASE)) }
    }

    /**
     * Format ISO date to display format
     * "2024-12-09" -> "09/12/2024"
     */
    private fun formatDateFromISO(isoDate: String): String {
        return try {
            val parts = isoDate.split("-")
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Format ISO date to short display format
     * "2024-12-09" -> "09/12/24"
     */
    private fun formatDateToDisplayShort(isoDate: String): String {
        return try {
            val parts = isoDate.split("-")
            "${parts[2]}/${parts[1]}/${parts[0].takeLast(2)}"
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Get month label from ISO date
     * "2024-12-09" -> "DIC"
     */
    private fun getMonthLabel(isoDate: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MMM", Locale("es", "ES"))
            val date = dateFormat.parse(isoDate)
            monthFormat.format(date).uppercase()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Convert datetime string to ISO date
     * "2024-12-09T10:30:00" -> "2024-12-09"
     */
    private fun convertToISODate(dateTime: String): String {
        return try {
            dateTime.split("T")[0]
        } catch (e: Exception) {
            dateTime
        }
    }
}
