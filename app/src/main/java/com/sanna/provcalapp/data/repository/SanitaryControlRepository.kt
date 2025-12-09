package com.sanna.provcalapp.data.repository

import android.content.Context
import com.sanna.provcalapp.data.local.SanitaryControlManager
import com.sanna.provcalapp.data.models.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for Sanitary Control (Health Policy) operations
 * Currently uses mock data - ready for GraphQL backend integration
 *
 * Constructor supports dependency injection for testing
 */
class SanitaryControlRepository(
    private val context: Context
) {

    private val localManager = SanitaryControlManager(context)

    companion object {
        // Mock data constants
        private const val POLICY_ID_PEST = "policy_001"
        private const val POLICY_ID_WASTE = "policy_002"
        private const val POLICY_ID_DINING = "policy_003"
    }

    /**
     * Get all active health policies
     * TODO: Replace with GraphQL query when backend is ready
     */
    suspend fun getPolicies(): Result<List<HealthPolicy>> {
        return try {
            delay(500) // Simulate network delay

            val policies = listOf(
                HealthPolicy(
                    id = POLICY_ID_PEST,
                    name = "Control de Plagas",
                    description = "Inspección y control de plagas en instalaciones",
                    type = PolicyType.PEST_CONTROL,
                    isActive = true
                ),
                HealthPolicy(
                    id = POLICY_ID_WASTE,
                    name = "Manejo de Residuos",
                    description = "Gestión y disposición adecuada de residuos",
                    type = PolicyType.WASTE_MANAGEMENT,
                    isActive = true
                ),
                HealthPolicy(
                    id = POLICY_ID_DINING,
                    name = "Saneamiento del Comedor",
                    description = "Limpieza y sanidad del área de comedor",
                    type = PolicyType.DINING_SANITATION,
                    isActive = true
                )
            )

            localManager.saveLastSyncTimestamp()
            Result.Success(policies)

        } catch (e: Exception) {
            Result.Error("Error al obtener políticas: ${e.message}", e)
        }
    }

    /**
     * Get revision history for a specific policy
     * @param policyId Policy identifier
     * @param filterMonths Number of months to filter (null = all history, 6, 12, 24)
     * TODO: Replace with GraphQL query when backend is ready
     */
    suspend fun getRevisionHistory(
        policyId: String,
        filterMonths: Int? = null
    ): Result<List<RevisionHistoryItem>> {
        return try {
            delay(500) // Simulate network delay

            val allHistory = generateMockHistory(policyId)

            // Apply time filter if specified
            val filteredHistory = if (filterMonths != null) {
                val cutoffDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -filterMonths)
                }
                val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

                allHistory.filter { item ->
                    try {
                        val itemDate = dateFormat.parse(item.date)
                        itemDate?.after(cutoffDate.time) ?: false
                    } catch (e: Exception) {
                        true // Include if date parsing fails
                    }
                }
            } else {
                allHistory
            }

            Result.Success(filteredHistory)

        } catch (e: Exception) {
            Result.Error("Error al obtener historial: ${e.message}", e)
        }
    }

    /**
     * Get next scheduled revision date for a policy
     * TODO: Replace with GraphQL query when backend is ready
     */
    suspend fun getNextRevisionDate(policyId: String): Result<String?> {
        return try {
            delay(300) // Simulate network delay

            // Check if we have a cached next date
            val cachedDate = localManager.getNextRevisionDate(policyId)
            if (cachedDate != null) {
                return Result.Success(cachedDate)
            }

            // Generate next revision date (1 month from now)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, 1)
            val nextDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

            localManager.saveNextRevisionDate(policyId, nextDate)
            Result.Success(nextDate)

        } catch (e: Exception) {
            Result.Error("Error al obtener próxima revisión: ${e.message}", e)
        }
    }

    /**
     * Schedule/confirm a revision
     * TODO: Replace with GraphQL mutation when backend is ready
     */
    suspend fun scheduleRevision(policyId: String, date: String): Result<Boolean> {
        return try {
            delay(500) // Simulate network delay

            localManager.saveNextRevisionDate(policyId, date)
            Result.Success(true)

        } catch (e: Exception) {
            Result.Error("Error al programar revisión: ${e.message}", e)
        }
    }

    /**
     * Submit a new revision (simple form - POLÍTICAS 3)
     * TODO: Replace with GraphQL mutation when backend is ready
     */
    suspend fun submitRevision(input: SubmitRevisionInput): Result<SubmitRevisionResponse> {
        return try {
            delay(700) // Simulate network delay

            val revisionId = "rev_${System.currentTimeMillis()}"

            // Calculate next revision date (1 month from submission)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, 1)
            val nextDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

            localManager.saveNextRevisionDate(input.policyId, nextDate)
            localManager.saveLastRevisionDate(input.policyId, input.date)

            Result.Success(
                SubmitRevisionResponse(
                    success = true,
                    message = "Revisión registrada exitosamente",
                    revisionId = revisionId,
                    nextRevisionDate = nextDate
                )
            )

        } catch (e: Exception) {
            Result.Error("Error al enviar revisión: ${e.message}", e)
        }
    }

    /**
     * Get a specific revision by ID
     * TODO: Replace with GraphQL query when backend is ready
     */
    suspend fun getRevisionById(revisionId: String): Result<HealthRevision> {
        return try {
            delay(400) // Simulate network delay

            // Mock data for a sample revision
            val revision = HealthRevision(
                id = revisionId,
                policyId = POLICY_ID_PEST,
                userId = "user_001",
                date = "2024-11-11",
                isConforme = false,
                observation = "En la parte del costado de la freidora se encontraron desechos de...",
                incidentTypeId = "incident_001",
                companyId = "company_001",
                createdAt = "2024-11-11T10:30:00"
            )

            Result.Success(revision)

        } catch (e: Exception) {
            Result.Error("Error al obtener revisión: ${e.message}", e)
        }
    }

    /**
     * Get specialized companies by service type
     * TODO: Replace with GraphQL query when backend is ready
     */
    suspend fun getCompanies(serviceType: String? = null): Result<List<Company>> {
        return try {
            delay(400) // Simulate network delay

            val allCompanies = generateMockCompanies()

            val filtered = if (serviceType != null) {
                allCompanies.filter { it.serviceType == serviceType }
            } else {
                allCompanies
            }

            Result.Success(filtered)

        } catch (e: Exception) {
            Result.Error("Error al obtener empresas: ${e.message}", e)
        }
    }

    /**
     * Get company by ID
     * TODO: Replace with GraphQL query when backend is ready
     */
    suspend fun getCompanyById(companyId: String): Result<Company> {
        return try {
            delay(300) // Simulate network delay

            val company = Company(
                id = companyId,
                socialReason = "Servicios especiales de Limpieza Ginyu",
                ruc = "20123456789",
                phone = "987654321",
                email = "contacto@ginyu.com",
                serviceType = "Control de plagas"
            )

            Result.Success(company)

        } catch (e: Exception) {
            Result.Error("Error al obtener empresa: ${e.message}", e)
        }
    }

    /**
     * Get incident types for a specific policy
     * TODO: Replace with GraphQL query when backend is ready
     */
    suspend fun getIncidentTypes(policyId: String): Result<List<IncidentType>> {
        return try {
            delay(300) // Simulate network delay

            val types = when (policyId) {
                POLICY_ID_PEST -> listOf(
                    IncidentType("inc_001", policyId, "Plaga de cucarachas", null),
                    IncidentType("inc_002", policyId, "Plaga de roedores", null),
                    IncidentType("inc_003", policyId, "Plaga de hormigas", null),
                    IncidentType("inc_004", policyId, "Plaga de moscas", null)
                )
                POLICY_ID_WASTE -> listOf(
                    IncidentType("inc_005", policyId, "Contenedores insuficientes", null),
                    IncidentType("inc_006", policyId, "Mal manejo de residuos orgánicos", null),
                    IncidentType("inc_007", policyId, "Falta de señalización", null)
                )
                POLICY_ID_DINING -> listOf(
                    IncidentType("inc_008", policyId, "Superficies sucias", null),
                    IncidentType("inc_009", policyId, "Utensilios mal lavados", null),
                    IncidentType("inc_010", policyId, "Área de cocina desordenada", null)
                )
                else -> emptyList()
            }

            Result.Success(types)

        } catch (e: Exception) {
            Result.Error("Error al obtener tipos de incidencia: ${e.message}", e)
        }
    }

    // ==================== MOCK DATA GENERATORS ====================

    private fun generateMockHistory(policyId: String): List<RevisionHistoryItem> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale("es", "ES")).apply {
            calendar.time = Date()
        }

        val history = mutableListOf<RevisionHistoryItem>()

        // Generate 12 months of history
        for (i in 0 until 12) {
            calendar.add(Calendar.MONTH, -1)
            val date = dateFormat.format(calendar.time)
            val month = monthFormat.format(calendar.time).uppercase()

            // Alternate between conforme/inconforme for variety
            val isConforme = (i % 3) != 1

            history.add(
                RevisionHistoryItem(
                    id = "rev_${policyId}_$i",
                    date = date,
                    monthLabel = month,
                    isConforme = isConforme,
                    observation = if (!isConforme) "Observación de la revisión" else null,
                    incidentType = if (!isConforme) "Plaga de cucarachas" else null,
                    company = if (!isConforme) Company(
                        "company_001",
                        "Servicios especiales de Limpieza Ginyu",
                        "20123456789",
                        "987654321",
                        "contacto@ginyu.com",
                        "Control de plagas"
                    ) else null,
                    hasIncident = !isConforme
                )
            )
        }

        return history
    }

    private fun generateMockCompanies(): List<Company> {
        return listOf(
            Company(
                id = "company_001",
                socialReason = "Servicios especiales de Limpieza Ginyu",
                ruc = "20123456789",
                phone = "987654321",
                email = "contacto@ginyu.com",
                serviceType = "Control de plagas"
            ),
            Company(
                id = "company_002",
                socialReason = "Fumigaciones El Escorpión SAC",
                ruc = "20987654321",
                phone = "965432178",
                email = "ventas@escorpion.com",
                serviceType = "Control de plagas"
            ),
            Company(
                id = "company_003",
                socialReason = "Ecogestión Residuos SAC",
                ruc = "20456789123",
                phone = "954321876",
                email = "info@ecogestion.com",
                serviceType = "Gestión de residuos"
            ),
            Company(
                id = "company_004",
                socialReason = "Limpieza Total EIRL",
                ruc = "20321654987",
                phone = "943218765",
                email = "contacto@limpiezatotal.com",
                serviceType = "Limpieza y saneamiento"
            ),
            Company(
                id = "company_005",
                socialReason = "Sanitaria del Norte SAC",
                ruc = "20789456123",
                phone = "932187654",
                email = "atencion@sanitarianorte.com",
                serviceType = "Limpieza y saneamiento"
            )
        )
    }
}
