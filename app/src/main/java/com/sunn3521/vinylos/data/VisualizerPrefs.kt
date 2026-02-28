package com.sunn3521.vinylos.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sunn3521.vinylos.ui.AODColorMode
import com.sunn3521.vinylos.ui.AODPosition
import com.sunn3521.vinylos.ui.VisualizerSettings
import com.sunn3521.vinylos.ui.VisualizerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "visualizer_settings")

class VisualizerPrefs(private val context: Context) {

    private object Keys {
        val TYPE = stringPreferencesKey("type")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val THICKNESS = floatPreferencesKey("thickness")
        val COLOR = intPreferencesKey("color")
        val GLOW = booleanPreferencesKey("glow")
        val GLOW_AMOUNT = floatPreferencesKey("glow_amount")
        val TRANSPARENCY = floatPreferencesKey("transparency")
        val SPEED = floatPreferencesKey("speed")
        val AMOLED = booleanPreferencesKey("amoled")
        val OFFSET_X = floatPreferencesKey("offset_x")
        val OFFSET_Y = floatPreferencesKey("offset_y")
        
        // AOD Keys
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_TITLE = booleanPreferencesKey("show_title")
        val CLOCK_POS = stringPreferencesKey("clock_pos")
        val TITLE_POS = stringPreferencesKey("title_pos")
        val VIZ_POS = stringPreferencesKey("viz_pos")
        val CLOCK_SIZE = floatPreferencesKey("clock_size")
        val TITLE_SIZE = floatPreferencesKey("title_size")
        val VIZ_SIZE = floatPreferencesKey("viz_size")
        val COLOR_MODE = stringPreferencesKey("color_mode")
        val AOD_ENABLED = booleanPreferencesKey("aod_enabled")
        val CHARGING_ONLY = booleanPreferencesKey("charging_only")
        val LOW_BATT_DISABLE = booleanPreferencesKey("low_batt_disable")
        val BURN_IN_PROTECTION = booleanPreferencesKey("burn_in_protection")
        val MIRRORED = booleanPreferencesKey("mirrored")
    }

    val settings: Flow<VisualizerSettings> = context.dataStore.data.map { prefs ->
        VisualizerSettings(
            type = VisualizerType.valueOf(prefs[Keys.TYPE] ?: VisualizerType.BAR.name),
            sensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
            thickness = prefs[Keys.THICKNESS] ?: 14f,
            color = Color(prefs[Keys.COLOR] ?: Color.Red.toArgb()),
            glow = prefs[Keys.GLOW] ?: true,
            glowAmount = prefs[Keys.GLOW_AMOUNT] ?: 0.5f,
            transparency = prefs[Keys.TRANSPARENCY] ?: 1.0f,
            speed = prefs[Keys.SPEED] ?: 1.0f,
            isAmoled = prefs[Keys.AMOLED] ?: true,
            offsetX = prefs[Keys.OFFSET_X] ?: 0f,
            offsetY = prefs[Keys.OFFSET_Y] ?: 0f,
            
            showClock = prefs[Keys.SHOW_CLOCK] ?: true,
            showSongTitle = prefs[Keys.SHOW_TITLE] ?: true,
            clockPosition = AODPosition.valueOf(prefs[Keys.CLOCK_POS] ?: AODPosition.TOP.name),
            titlePosition = AODPosition.valueOf(prefs[Keys.TITLE_POS] ?: AODPosition.BOTTOM.name),
            visualizerPosition = AODPosition.valueOf(prefs[Keys.VIZ_POS] ?: AODPosition.CENTER.name),
            clockSize = prefs[Keys.CLOCK_SIZE] ?: 80f,
            titleSize = prefs[Keys.TITLE_SIZE] ?: 24f,
            visualizerSize = prefs[Keys.VIZ_SIZE] ?: 1f,
            colorMode = AODColorMode.valueOf(prefs[Keys.COLOR_MODE] ?: AODColorMode.AUTO.name),
            aodEnabled = prefs[Keys.AOD_ENABLED] ?: true,
            onlyWhenCharging = prefs[Keys.CHARGING_ONLY] ?: false,
            disableLowBattery = prefs[Keys.LOW_BATT_DISABLE] ?: true,
            burnInProtection = prefs[Keys.BURN_IN_PROTECTION] ?: true,
            mirrored = prefs[Keys.MIRRORED] ?: true
        )
    }

    suspend fun updateSettings(settings: VisualizerSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TYPE] = settings.type.name
            prefs[Keys.SENSITIVITY] = settings.sensitivity
            prefs[Keys.THICKNESS] = settings.thickness
            prefs[Keys.COLOR] = settings.color.toArgb()
            prefs[Keys.GLOW] = settings.glow
            prefs[Keys.GLOW_AMOUNT] = settings.glowAmount
            prefs[Keys.TRANSPARENCY] = settings.transparency
            prefs[Keys.SPEED] = settings.speed
            prefs[Keys.AMOLED] = settings.isAmoled
            prefs[Keys.OFFSET_X] = settings.offsetX
            prefs[Keys.OFFSET_Y] = settings.offsetY
            
            prefs[Keys.SHOW_CLOCK] = settings.showClock
            prefs[Keys.SHOW_TITLE] = settings.showSongTitle
            prefs[Keys.CLOCK_POS] = settings.clockPosition.name
            prefs[Keys.TITLE_POS] = settings.titlePosition.name
            prefs[Keys.VIZ_POS] = settings.visualizerPosition.name
            prefs[Keys.CLOCK_SIZE] = settings.clockSize
            prefs[Keys.TITLE_SIZE] = settings.titleSize
            prefs[Keys.VIZ_SIZE] = settings.visualizerSize
            prefs[Keys.COLOR_MODE] = settings.colorMode.name
            prefs[Keys.AOD_ENABLED] = settings.aodEnabled
            prefs[Keys.CHARGING_ONLY] = settings.onlyWhenCharging
            prefs[Keys.LOW_BATT_DISABLE] = settings.disableLowBattery
            prefs[Keys.BURN_IN_PROTECTION] = settings.burnInProtection
            prefs[Keys.MIRRORED] = settings.mirrored
        }
    }
}
