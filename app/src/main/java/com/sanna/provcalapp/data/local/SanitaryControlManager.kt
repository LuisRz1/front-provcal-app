package com.sanna.provcalapp.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages local caching and persistence for sanitary control data
 * Uses SharedPreferences to store temporary states and cached data
 */
class SanitaryControlManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "sanitary_control_prefs"

        // Keys for stored data
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_SELECTED_POLICY_ID = "selected_policy_id"
        private const val KEY_SELECTED_POLICY_TYPE = "selected_policy_type"
        private const val KEY_PENDING_REVISION_ID = "pending_revision_id"
        private const val KEY_NEXT_REVISION_DATE_PREFIX = "next_revision_date_"
        private const val KEY_LAST_REVISION_DATE_PREFIX = "last_revision_date_"

        // Cache expiration (24 hours)
        private const val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L
    }

    /**
     * Save the last sync timestamp
     */
    fun saveLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }

    /**
     * Get the last sync timestamp
     */
    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }

    /**
     * Check if cached data is still valid (not expired)
     */
    fun isCacheValid(): Boolean {
        val lastSync = getLastSyncTimestamp()
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastSync) < CACHE_EXPIRATION_MS
    }

    /**
     * Save the currently selected policy for navigation
     */
    fun saveSelectedPolicy(policyId: String, policyType: String) {
        prefs.edit()
            .putString(KEY_SELECTED_POLICY_ID, policyId)
            .putString(KEY_SELECTED_POLICY_TYPE, policyType)
            .apply()
    }

    /**
     * Get the currently selected policy ID
     */
    fun getSelectedPolicyId(): String? {
        return prefs.getString(KEY_SELECTED_POLICY_ID, null)
    }

    /**
     * Get the currently selected policy type
     */
    fun getSelectedPolicyType(): String? {
        return prefs.getString(KEY_SELECTED_POLICY_TYPE, null)
    }

    /**
     * Clear selected policy
     */
    fun clearSelectedPolicy() {
        prefs.edit()
            .remove(KEY_SELECTED_POLICY_ID)
            .remove(KEY_SELECTED_POLICY_TYPE)
            .apply()
    }

    /**
     * Save a pending revision ID (for multi-step form completion)
     */
    fun savePendingRevisionId(revisionId: String?) {
        if (revisionId != null) {
            prefs.edit().putString(KEY_PENDING_REVISION_ID, revisionId).apply()
        } else {
            prefs.edit().remove(KEY_PENDING_REVISION_ID).apply()
        }
    }

    /**
     * Get pending revision ID
     */
    fun getPendingRevisionId(): String? {
        return prefs.getString(KEY_PENDING_REVISION_ID, null)
    }

    /**
     * Clear pending revision
     */
    fun clearPendingRevision() {
        prefs.edit().remove(KEY_PENDING_REVISION_ID).apply()
    }

    /**
     * Save next revision date for a specific policy
     */
    fun saveNextRevisionDate(policyId: String, date: String) {
        prefs.edit().putString(KEY_NEXT_REVISION_DATE_PREFIX + policyId, date).apply()
    }

    /**
     * Get next revision date for a specific policy
     */
    fun getNextRevisionDate(policyId: String): String? {
        return prefs.getString(KEY_NEXT_REVISION_DATE_PREFIX + policyId, null)
    }

    /**
     * Save last revision date for a specific policy
     */
    fun saveLastRevisionDate(policyId: String, date: String) {
        prefs.edit().putString(KEY_LAST_REVISION_DATE_PREFIX + policyId, date).apply()
    }

    /**
     * Get last revision date for a specific policy
     */
    fun getLastRevisionDate(policyId: String): String? {
        return prefs.getString(KEY_LAST_REVISION_DATE_PREFIX + policyId, null)
    }

    /**
     * Clear all sanitary control data (on logout or data reset)
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Clear only cache-related data (keep user selections)
     */
    fun clearCache() {
        prefs.edit()
            .remove(KEY_LAST_SYNC_TIMESTAMP)
            .apply()
    }
}
