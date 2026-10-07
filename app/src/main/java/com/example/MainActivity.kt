package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.EcoUiState
import com.example.ui.EcoViewModel
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.CleanResultScreen
import com.example.ui.screens.ErrorScreen
import com.example.ui.screens.LoadingScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.theme.EcoScanTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EcoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            EcoScanTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EcoScanApp(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }
}

@Composable
fun EcoScanApp(
    viewModel: EcoViewModel,
    isDarkMode: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()

    // Gallery Picker Contract (zero-permission Android Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeImageUri(uri)
        }
    }

    // BackHandler based on current state
    BackHandler(enabled = uiState !is EcoUiState.Home) {
        viewModel.navigateToHome()
    }

    when (val state = uiState) {
        is EcoUiState.Home -> {
            MainScreen(
                isDarkMode = isDarkMode,
                onToggleTheme = { viewModel.toggleTheme() },
                onCaptureClick = { viewModel.navigateToCamera() },
                onGalleryClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        is EcoUiState.Camera -> {
            CameraScreen(
                onPhotoCaptured = { uri -> viewModel.analyzeImageUri(uri) },
                onBackClick = { viewModel.navigateToHome() },
                onGalleryFallback = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        is EcoUiState.Loading -> {
            LoadingScreen(
                imageUri = state.imageUri,
                factText = state.fact
            )
        }

        is EcoUiState.Success -> {
            ResultScreen(
                result = state.result,
                isDarkMode = isDarkMode,
                onRetakeClick = { viewModel.navigateToCamera() },
                onBackClick = { viewModel.navigateToHome() }
            )
        }

        is EcoUiState.Clean -> {
            CleanResultScreen(
                result = state.result,
                onTryAgainClick = { viewModel.navigateToCamera() },
                onBackClick = { viewModel.navigateToHome() }
            )
        }

        is EcoUiState.Error -> {
            ErrorScreen(
                errorMessage = state.message,
                lastImageUri = state.lastImageUri,
                onRetryClick = {
                    state.lastImageUri?.let { viewModel.analyzeImageUri(it) }
                        ?: viewModel.navigateToHome()
                },
                onBackClick = { viewModel.navigateToHome() }
            )
        }
    }
}
