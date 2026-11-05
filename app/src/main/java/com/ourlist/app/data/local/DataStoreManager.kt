package com.ourlist.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ourlist.app.data.model.SortOption
import com.ourlist.app.data.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "our_list_prefs")

class DataStoreManager(private val context: Context) {

    private object Keys {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val DEVICE_SEQUENCE = longPreferencesKey("device_sequence")
        val USER_SUB = stringPreferencesKey("user_sub")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val PARTNER_SUB = stringPreferencesKey("partner_sub")
        val PARTNER_EMAIL = stringPreferencesKey("partner_email")
        val PARTNER_NAME = stringPreferencesKey("partner_name")
        val SHARED_FOLDER_ID = stringPreferencesKey("shared_folder_id")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val MANUAL_RANKS_JSON = stringPreferencesKey("manual_ranks_json")
        val REMINDER_ENABLED_JSON = stringPreferencesKey("reminder_enabled_json")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val BACKUP_RECOVERY_CODE = stringPreferencesKey("backup_recovery_code")
        val GOOGLE_AUTH_TOKEN = stringPreferencesKey("google_auth_token")
        val COUPLE_CODE = stringPreferencesKey("couple_code")
        val FIREBASE_RTDB_URL = stringPreferencesKey("firebase_rtdb_url")
        val SAF_BACKUP_TREE_URI = stringPreferencesKey("saf_backup_tree_uri")
        val SAF_BACKUP_ENABLED = booleanPreferencesKey("saf_backup_enabled")
        val LAST_SAF_BACKUP_TIME = longPreferencesKey("last_saf_backup_time")
        val IS_PARTNER_CONNECTED = booleanPreferencesKey("is_partner_connected")
        val DATING_START_DATE = stringPreferencesKey("dating_start_date")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val deviceIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEVICE_ID] ?: ""
    }

    suspend fun getOrCreateDeviceId(): String {
        val current = context.dataStore.data.first()[Keys.DEVICE_ID]
        if (!current.isNullOrEmpty()) return current
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.DEVICE_ID] = newId }
        return newId
    }

    suspend fun getNextDeviceSequence(): Long {
        var seq = 1L
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.DEVICE_SEQUENCE] ?: 0L
            seq = cur + 1
            prefs[Keys.DEVICE_SEQUENCE] = seq
        }
        return seq
    }

    val userSubFlow: Flow<String> = context.dataStore.data.map { it[Keys.USER_SUB] ?: "" }
    val userEmailFlow: Flow<String> = context.dataStore.data.map { it[Keys.USER_EMAIL] ?: "" }
    val userNameFlow: Flow<String> = context.dataStore.data.map { it[Keys.USER_NAME] ?: "나" }

    val partnerSubFlow: Flow<String> = context.dataStore.data.map { it[Keys.PARTNER_SUB] ?: "" }
    val partnerEmailFlow: Flow<String> = context.dataStore.data.map { it[Keys.PARTNER_EMAIL] ?: "" }
    val partnerNameFlow: Flow<String> = context.dataStore.data.map { it[Keys.PARTNER_NAME] ?: "상대방" }

    val sharedFolderIdFlow: Flow<String> = context.dataStore.data.map { it[Keys.SHARED_FOLDER_ID] ?: "" }
    val coupleCodeFlow: Flow<String> = context.dataStore.data.map { it[Keys.COUPLE_CODE] ?: "" }
    val firebaseRtdbUrlFlow: Flow<String> = context.dataStore.data.map {
        it[Keys.FIREBASE_RTDB_URL] ?: "https://ourlist-4462e-default-rtdb.firebaseio.com"
    }
    val safBackupTreeUriFlow: Flow<String> = context.dataStore.data.map { it[Keys.SAF_BACKUP_TREE_URI] ?: "" }
    val safBackupEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.SAF_BACKUP_ENABLED] ?: true }
    val lastSafBackupTimeFlow: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_SAF_BACKUP_TIME] ?: 0L }
    val isPartnerConnectedFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_PARTNER_CONNECTED] ?: false }
    val datingStartDateFlow: Flow<String> = context.dataStore.data.map { it[Keys.DATING_START_DATE] ?: "" }

    val viewModeFlow: Flow<ViewMode> = context.dataStore.data.map {
        val name = it[Keys.VIEW_MODE] ?: ViewMode.LIST.name
        try { ViewMode.valueOf(name) } catch (e: Exception) { ViewMode.LIST }
    }

    val sortOptionFlow: Flow<SortOption> = context.dataStore.data.map {
        val name = it[Keys.SORT_OPTION] ?: SortOption.TARGET_DATE.name
        try { SortOption.valueOf(name) } catch (e: Exception) { SortOption.TARGET_DATE }
    }

    val manualRanksFlow: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.MANUAL_RANKS_JSON] ?: "{}"
        try { json.decodeFromString<Map<String, Int>>(raw) } catch (e: Exception) { emptyMap() }
    }

    val reminderEnabledMapFlow: Flow<Map<String, Boolean>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.REMINDER_ENABLED_JSON] ?: "{}"
        try { json.decodeFromString<Map<String, Boolean>>(raw) } catch (e: Exception) { emptyMap() }
    }

    val autoBackupEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.AUTO_BACKUP_ENABLED] ?: false
    }

    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_SYNC_TIME] ?: 0L }
    val lastBackupTimeFlow: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_BACKUP_TIME] ?: 0L }
    val recoveryCodeFlow: Flow<String> = context.dataStore.data.map { it[Keys.BACKUP_RECOVERY_CODE] ?: "" }

    suspend fun setUserInfo(sub: String, email: String, name: String) {
        context.dataStore.edit {
            it[Keys.USER_SUB] = sub
            it[Keys.USER_EMAIL] = email
            it[Keys.USER_NAME] = name
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit {
            it[Keys.USER_NAME] = name
        }
    }

    suspend fun setPartnerInfo(sub: String, email: String, name: String) {
        context.dataStore.edit {
            it[Keys.PARTNER_SUB] = sub
            it[Keys.PARTNER_EMAIL] = email
            it[Keys.PARTNER_NAME] = name
        }
    }

    suspend fun setPartnerName(name: String) {
        context.dataStore.edit {
            it[Keys.PARTNER_NAME] = name
        }
    }

    suspend fun setSharedFolderId(folderId: String) {
        context.dataStore.edit { it[Keys.SHARED_FOLDER_ID] = folderId }
    }

    suspend fun setCoupleCode(code: String) {
        context.dataStore.edit { it[Keys.COUPLE_CODE] = code }
    }

    suspend fun setFirebaseRtdbUrl(url: String) {
        context.dataStore.edit { it[Keys.FIREBASE_RTDB_URL] = url }
    }

    suspend fun setSafBackupTreeUri(uri: String) {
        context.dataStore.edit { it[Keys.SAF_BACKUP_TREE_URI] = uri }
    }

    suspend fun setSafBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SAF_BACKUP_ENABLED] = enabled }
    }

    suspend fun setLastSafBackupTime(time: Long) {
        context.dataStore.edit { it[Keys.LAST_SAF_BACKUP_TIME] = time }
    }

    suspend fun setPartnerConnected(connected: Boolean) {
        context.dataStore.edit { it[Keys.IS_PARTNER_CONNECTED] = connected }
    }

    suspend fun setDatingStartDate(date: String) {
        context.dataStore.edit { it[Keys.DATING_START_DATE] = date }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setSortOption(option: SortOption) {
        context.dataStore.edit { it[Keys.SORT_OPTION] = option.name }
    }

    suspend fun setManualRank(itemId: String, rank: Int) {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.MANUAL_RANKS_JSON] ?: "{}"
            val map = try {
                json.decodeFromString<Map<String, Int>>(raw).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
            map[itemId] = rank
            prefs[Keys.MANUAL_RANKS_JSON] = json.encodeToString(map)
        }
    }

    suspend fun setManualRanks(ranks: Map<String, Int>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MANUAL_RANKS_JSON] = json.encodeToString(ranks)
        }
    }

    suspend fun setItemReminderEnabled(itemId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.REMINDER_ENABLED_JSON] ?: "{}"
            val map = try {
                json.decodeFromString<Map<String, Boolean>>(raw).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
            map[itemId] = enabled
            prefs[Keys.REMINDER_ENABLED_JSON] = json.encodeToString(map)
        }
    }

    suspend fun isReminderEnabled(itemId: String): Boolean {
        val raw = context.dataStore.data.first()[Keys.REMINDER_ENABLED_JSON] ?: "{}"
        return try {
            val map = json.decodeFromString<Map<String, Boolean>>(raw)
            map[itemId] ?: true // default true if targetDate is set
        } catch (e: Exception) {
            true
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setLastSyncTime(time: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TIME] = time }
    }

    suspend fun setLastBackupTime(time: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_TIME] = time }
    }

    suspend fun setRecoveryCode(code: String) {
        context.dataStore.edit { it[Keys.BACKUP_RECOVERY_CODE] = code }
    }

    val googleAuthTokenFlow: Flow<String> = context.dataStore.data.map { it[Keys.GOOGLE_AUTH_TOKEN] ?: "" }

    suspend fun setGoogleAuthToken(token: String) {
        context.dataStore.edit { it[Keys.GOOGLE_AUTH_TOKEN] = token }
    }
}
