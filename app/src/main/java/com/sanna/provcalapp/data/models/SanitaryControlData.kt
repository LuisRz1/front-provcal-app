package com.sanna.provcalapp.data.models

/**
 * Data models for Sanitary Control (Health Policy) feature
 * Based on ER diagram: PolíticaSanidad, RevisionSanidad, TipoIncidencia, Empresa
 */

/**
 * Represents a health/sanitary policy type
 * Maps to: PolíticaSanidad table
 */
data class HealthPolicy(
    val id: String,
    val name: String,
    val description: String,
    val type: PolicyType,
    val isActive: Boolean = true
)

enum class PolicyType {
    PEST_CONTROL,      // Control de Plagas
    WASTE_MANAGEMENT,  // Manejo de Residuos
    DINING_SANITATION  // Saneamiento del Comedor
}

/**
 * Represents a health inspection/revision record
 * Maps to: RevisionSanidad table
 */
data class HealthRevision(
    val id: String,
    val policyId: String,
    val userId: String,
    val date: String, // ISO 8601 format
    val isConforme: Boolean, // true = conforme, false = inconforme
    val observation: String?,
    val incidentTypeId: String?,
    val companyId: String?,
    val createdAt: String
)

/**
 * Simplified revision history item for list display
 */
data class RevisionHistoryItem(
    val id: String,
    val date: String, // Format: "12/11/24"
    val monthLabel: String, // Format: "NOV" or "OCT"
    val isConforme: Boolean,
    val observation: String?,
    val incidentType: String?,
    val company: Company?,
    val hasIncident: Boolean = false
)

/**
 * Represents an incident type
 * Maps to: TipoIncidencia table
 */
data class IncidentType(
    val id: String,
    val policyId: String,
    val name: String, // e.g., "Plaga de cucarachas"
    val description: String?
)

/**
 * Represents a specialized service company
 * Maps to: Empresa table
 */
data class Company(
    val id: String,
    val socialReason: String, // razon_social
    val ruc: String,
    val phone: String?,
    val email: String?,
    val serviceType: String // e.g., "Control de plagas", "Limpieza"
)

/**
 * Input model for submitting a new revision
 */
data class SubmitRevisionInput(
    val policyId: String,
    val date: String,
    val isConforme: Boolean,
    val observation: String?,
    val incidentTypeId: String?,
    val companyId: String?
)

/**
 * Response model for revision submission
 */
data class SubmitRevisionResponse(
    val success: Boolean,
    val message: String,
    val revisionId: String?,
    val nextRevisionDate: String?
)
