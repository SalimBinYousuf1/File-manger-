package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salim_preferences")

class SalimPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
        val KEY_SORT_FIELD = stringPreferencesKey("sort_field")
        val KEY_SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val KEY_SHOW_HIDDEN = booleanPreferencesKey("show_hidden_files")
        val KEY_CONFIRM_DELETE = booleanPreferencesKey("confirm_before_delete")
        val KEY_TRASH_RETENTION_DAYS = intPreferencesKey("trash_retention_days")
        val KEY_ENABLE_ROOT = booleanPreferencesKey("enable_root_access")
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_PASSCODE = stringPreferencesKey("app_passcode")
        val KEY_ROW_DENSITY = stringPreferencesKey("row_density")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { it[KEY_THEME] ?: "SYSTEM" }
    val rowDensityFlow: Flow<String> = context.dataStore.data.map { it[KEY_ROW_DENSITY] ?: "STANDARD" }
    val viewModeFlow: Flow<ViewMode> = context.dataStore.data.map {
        when (it[KEY_VIEW_MODE]) {
            "GRID" -> ViewMode.GRID
            else -> ViewMode.LIST
        }
    }
    val sortFieldFlow: Flow<SortField> = context.dataStore.data.map {
        try {
            SortField.valueOf(it[KEY_SORT_FIELD] ?: SortField.NAME.name)
        } catch (e: Exception) {
            SortField.NAME
        }
    }
    val sortDirectionFlow: Flow<SortDirection> = context.dataStore.data.map {
        try {
            SortDirection.valueOf(it[KEY_SORT_DIRECTION] ?: SortDirection.ASCENDING.name)
        } catch (e: Exception) {
            SortDirection.ASCENDING
        }
    }
    val showHiddenFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_HIDDEN] ?: false }
    val confirmDeleteFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_CONFIRM_DELETE] ?: true }
    val trashRetentionDaysFlow: Flow<Int> = context.dataStore.data.map { it[KEY_TRASH_RETENTION_DAYS] ?: 30 }
    val enableRootFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENABLE_ROOT] ?: false }
    val appLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_APP_LOCK_ENABLED] ?: false }
    val passcodeFlow: Flow<String?> = context.dataStore.data.map { it[KEY_PASSCODE] }

    suspend fun setTheme(theme: String) = context.dataStore.edit { it[KEY_THEME] = theme }
    suspend fun setViewMode(mode: ViewMode) = context.dataStore.edit { it[KEY_VIEW_MODE] = mode.name }
    suspend fun setSort(field: SortField, direction: SortDirection) = context.dataStore.edit {
        it[KEY_SORT_FIELD] = field.name
        it[KEY_SORT_DIRECTION] = direction.name
    }
    suspend fun setShowHidden(show: Boolean) = context.dataStore.edit { it[KEY_SHOW_HIDDEN] = show }
    suspend fun setConfirmDelete(confirm: Boolean) = context.dataStore.edit { it[KEY_CONFIRM_DELETE] = confirm }
    suspend fun setTrashRetentionDays(days: Int) = context.dataStore.edit { it[KEY_TRASH_RETENTION_DAYS] = days }
    suspend fun setEnableRoot(enable: Boolean) = context.dataStore.edit { it[KEY_ENABLE_ROOT] = enable }
    suspend fun setAppLock(enabled: Boolean, passcode: String?) = context.dataStore.edit {
        it[KEY_APP_LOCK_ENABLED] = enabled
        if (passcode != null) it[KEY_PASSCODE] = passcode
    }
    suspend fun setRowDensity(density: String) = context.dataStore.edit { it[KEY_ROW_DENSITY] = density }
}
