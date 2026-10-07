package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.api.GroqApiService
import com.example.data.config.EcoConfig
import com.example.data.model.EcoAnalysisResult
import com.example.util.ImageUtils

class EcoRepository(
    private val context: Context,
    private val apiService: GroqApiService = GroqApiService()
) {

    fun getApiKey(): String = EcoConfig.GROQ_API_KEY

    suspend fun analyzeImageUri(uri: Uri): Result<EcoAnalysisResult> {
        val processed = ImageUtils.processImage(context, uri)
            ?: return Result.failure(Exception("Не удалось обработать изображение"))

        val apiKey = getApiKey()
        val result = apiService.analyzeImage(processed.base64, apiKey)

        return result.map { analysis ->
            analysis.copy(localImagePath = processed.filePath)
        }
    }
}
