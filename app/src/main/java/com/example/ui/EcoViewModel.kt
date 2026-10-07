package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.EcoAnalysisResult
import com.example.data.repository.EcoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface EcoUiState {
    object Home : EcoUiState
    data class Camera(val flashOn: Boolean = false, val isFront: Boolean = false) : EcoUiState
    data class Loading(val imageUri: Uri, val fact: String) : EcoUiState
    data class Success(val result: EcoAnalysisResult) : EcoUiState
    data class Clean(val result: EcoAnalysisResult) : EcoUiState
    data class Error(val message: String, val lastImageUri: Uri?) : EcoUiState
}

class EcoViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ecoscan_theme_prefs", Context.MODE_PRIVATE)
    private val repository = EcoRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<EcoUiState>(EcoUiState.Home)
    val uiState: StateFlow<EcoUiState> = _uiState.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private var factRotationJob: Job? = null

    private val ecoFacts = listOf(
        "Ежегодно в Казахстане образуется свыше 4,5 млн тонн твердых бытовых отходов (ТБО).",
        "Период распада пластиковой бутылки на открытом воздухе может достигать 450 лет.",
        "Одна выброшенная батарейка может отравить до 20 м² почвы тяжелыми металлами.",
        "В Казахстане действует общенациональная экологическая программа 'Таза Қазақстан'.",
        "Переработка 1 тонны макулатуры спасает от вырубки до 17 взрослых деревьев."
    )

    fun toggleTheme() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        prefs.edit().putBoolean("is_dark_mode", newMode).apply()
        updateLauncherIcon(newMode)
    }

    private fun updateLauncherIcon(isDark: Boolean) {
        try {
            val app = getApplication<Application>()
            val pm = app.packageManager
            val defaultComp = ComponentName(app.packageName, "com.example.MainActivity")
            val darkComp = ComponentName(app.packageName, "com.example.MainActivityDark")

            if (isDark) {
                pm.setComponentEnabledSetting(
                    defaultComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    darkComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    darkComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    defaultComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun navigateToHome() {
        factRotationJob?.cancel()
        _uiState.value = EcoUiState.Home
    }

    fun navigateToCamera() {
        _uiState.value = EcoUiState.Camera()
    }

    fun analyzeImageUri(uri: Uri) {
        factRotationJob?.cancel()
        var currentFactIndex = 0
        _uiState.value = EcoUiState.Loading(uri, ecoFacts[currentFactIndex])

        // Rotate facts during analysis
        factRotationJob = viewModelScope.launch {
            while (isActive) {
                delay(3500)
                currentFactIndex = (currentFactIndex + 1) % ecoFacts.size
                val current = _uiState.value
                if (current is EcoUiState.Loading) {
                    _uiState.value = current.copy(fact = ecoFacts[currentFactIndex])
                }
            }
        }

        viewModelScope.launch {
            val result = repository.analyzeImageUri(uri)
            factRotationJob?.cancel()

            result.fold(
                onSuccess = { analysis ->
                    if (analysis.isTrash) {
                        _uiState.value = EcoUiState.Success(analysis)
                    } else {
                        _uiState.value = EcoUiState.Clean(analysis)
                    }
                },
                onFailure = { error ->
                    _uiState.value = EcoUiState.Error(
                        message = error.message ?: "Произошла ошибка при анализе фото",
                        lastImageUri = uri
                    )
                }
            )
        }
    }
}
