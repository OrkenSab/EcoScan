package com.example.data.api

import android.util.Log
import com.example.data.config.EcoConfig
import com.example.data.model.EcoAnalysisResult
import com.example.data.model.HarmDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class GroqApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(EcoConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(EcoConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(EcoConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "GroqApiService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val THINK_BLOCK_PATTERN = Pattern.compile("<think>[\\s\\S]*?<\\/think>", Pattern.CASE_INSENSITIVE)
        private val MARKDOWN_CODE_PATTERN = Pattern.compile("```(?:json)?([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE)
    }

    /**
     * Sends the image base64 to Groq Vision model and returns the parsed EcoAnalysisResult.
     * Retries once on parse error as per requirements.
     */
    suspend fun analyzeImage(
        imageBase64: String,
        apiKey: String
    ): Result<EcoAnalysisResult> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim().trimEnd('/')
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("GROQ_API_KEY не указан.")
            )
        }

        // Try primary request
        val firstAttempt = executeRequest(imageBase64, cleanKey, isRetry = false)
        if (firstAttempt.isSuccess) {
            return@withContext firstAttempt
        }

        // Check if we should retry
        val firstException = firstAttempt.exceptionOrNull()
        Log.w(TAG, "First attempt failed: ${firstException?.message}. Attempting 1 retry...")

        // Retry once
        val secondAttempt = executeRequest(imageBase64, apiKey, isRetry = true)
        if (secondAttempt.isSuccess) {
            return@withContext secondAttempt
        }

        return@withContext Result.failure(
            secondAttempt.exceptionOrNull() ?: firstException ?: Exception("Ошибка анализа изображения")
        )
    }

    private fun executeRequest(
        imageBase64: String,
        apiKey: String,
        isRetry: Boolean
    ): Result<EcoAnalysisResult> {
        return try {
            val requestBodyJson = buildRequestBodyJson(imageBase64, isRetry)
            val requestBody = requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(EcoConfig.API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            // Safe logging without exposing key or full base64
            Log.d(TAG, "Sending request to Groq (${EcoConfig.MODEL_ID}), retry=$isRetry")

            val response = client.newCall(request).execute()
            val statusCode = response.code
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Groq API error HTTP $statusCode")
                val errorMessage = parseErrorMessage(statusCode, responseBodyString)
                return Result.failure(IOException(errorMessage))
            }

            // Extract content from OpenAI-compatible response
            val rawContent = extractContentFromGroqResponse(responseBodyString)
            if (rawContent.isNullOrBlank()) {
                return Result.failure(IOException("Пустой ответ от нейросети"))
            }

            // Robust JSON extraction
            val parsedResult = extractAndParseJson(rawContent)
                ?: return Result.failure(IOException("Не удалось разобрать JSON-ответ модели"))

            Result.success(parsedResult)
        } catch (e: Exception) {
            Log.e(TAG, "Request execution failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun buildRequestBodyJson(imageBase64: String, isRetry: Boolean): JSONObject {
        val root = JSONObject()
        root.put("model", EcoConfig.MODEL_ID)
        root.put("max_tokens", EcoConfig.MAX_TOKENS)
        root.put("temperature", if (isRetry) 0.1 else 0.2)

        val messages = JSONArray()

        // System message
        val sysMsg = JSONObject()
        sysMsg.put("role", "system")
        sysMsg.put("content", EcoConfig.SYSTEM_PROMPT)
        messages.put(sysMsg)

        // User message with multimodal parts
        val userMsg = JSONObject()
        userMsg.put("role", "user")

        val userParts = JSONArray()

        val textPart = JSONObject()
        textPart.put("type", "text")
        textPart.put(
            "text",
            if (isRetry)
                "Внимание: верни ТОЛЬКО валидный JSON без вступительного текста и без <think> блоков! Проанализируй фото мусора:"
            else
                "Проанализируй фото мусора и верни результат строго в JSON формате согласно инструкции."
        )
        userParts.put(textPart)

        val imagePart = JSONObject()
        imagePart.put("type", "image_url")
        val imageUrlObj = JSONObject()
        imageUrlObj.put("url", "data:image/jpeg;base64,$imageBase64")
        imagePart.put("image_url", imageUrlObj)
        userParts.put(imagePart)

        userMsg.put("content", userParts)
        messages.put(userMsg)

        root.put("messages", messages)
        return root
    }

    private fun extractContentFromGroqResponse(responseJsonStr: String): String? {
        return try {
            val json = JSONObject(responseJsonStr)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message") ?: return null

            // Ignore "reasoning" field if present, only return final "content"
            if (message.has("content")) message.getString("content") else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract content choice: ${e.message}")
            null
        }
    }

    /**
     * Cleans <think> tags, markdown fences, and extracts JSON between { and }
     */
    fun extractAndParseJson(rawText: String): EcoAnalysisResult? {
        var cleaned = rawText

        // 1. Remove <think>...</think> blocks
        cleaned = THINK_BLOCK_PATTERN.matcher(cleaned).replaceAll("")

        // 2. Remove markdown code blocks if present
        val mdMatcher = MARKDOWN_CODE_PATTERN.matcher(cleaned)
        if (mdMatcher.find()) {
            cleaned = mdMatcher.group(1) ?: cleaned
        }

        // 3. Find first outer JSON object between '{' and '}'
        val firstBrace = cleaned.indexOf('{')
        val lastBrace = cleaned.lastIndexOf('}')

        if (firstBrace == -1 || lastBrace == -1 || lastBrace <= firstBrace) {
            Log.w(TAG, "No JSON braces found in response: $cleaned")
            return null
        }

        val jsonSubstring = cleaned.substring(firstBrace, lastBrace + 1).trim()

        return try {
            val obj = JSONObject(jsonSubstring)

            val isTrash = obj.optBoolean("is_trash", true)
            val rawScore = obj.optInt("pollution_score", if (isTrash) 5 else 0)
            // Bound strictly to 0..10
            val pollutionScore = rawScore.coerceIn(0, 10)

            val typesList = mutableListOf<String>()
            val typesArray = obj.optJSONArray("trash_types")
            if (typesArray != null) {
                for (i in 0 until typesArray.length()) {
                    val item = typesArray.optString(i)
                    if (!item.isNullOrBlank()) {
                        typesList.add(item.trim())
                    }
                }
            }
            if (isTrash && typesList.isEmpty()) {
                typesList.add("смешанный мусор")
            }

            val summary = obj.optString(
                "summary",
                if (isTrash) "На фотографии зафиксировано скопление бытового мусора."
                else "На снимке окружающая среда без следов мусора."
            )

            // Parse harm
            val harmObj = obj.optJSONObject("harm")
            val harmNature = harmObj?.optString("nature")
                ?.takeIf { it.isNotBlank() }
                ?: "Мусор загрязняет почвенно-растительный покров и угрожает фауне региона."
            val harmHealth = harmObj?.optString("health")
                ?.takeIf { it.isNotBlank() }
                ?: "Скопление отходов ухудшает санитарно-эпидемиологическую обстановку."
            val harmEconomy = harmObj?.optString("economy")
                ?.takeIf { it.isNotBlank() }
                ?: "Ликвидация несанкционированных отходов требует значительных бюджетных затрат."

            val harmDetails = HarmDetails(
                nature = harmNature,
                health = harmHealth,
                economy = harmEconomy
            )

            val advice = obj.optString(
                "advice",
                if (isTrash) "Соберите отходы и утилизируйте в специально отведенные контейнеры раздельного сбора."
                else "Продолжайте бережно относиться к чистоте природы Казахстана!"
            )

            EcoAnalysisResult(
                isTrash = isTrash,
                pollutionScore = pollutionScore,
                trashTypes = typesList,
                summary = summary,
                harm = harmDetails,
                advice = advice
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON string: ${e.message}")
            null
        }
    }

    private fun parseErrorMessage(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message")
            when {
                code == 401 -> "Неверный или отсутствующий Groq API ключ (HTTP 401)"
                code == 429 -> "Превышен лимит запросов Groq API (HTTP 429). Попробуйте через минуту или включите демо-режим."
                !message.isNullOrBlank() -> "Ошибка Groq ($code): $message"
                else -> "Ошибка сервера Groq (HTTP $code)"
            }
        } catch (e: Exception) {
            when (code) {
                401 -> "Неверный Groq API ключ (HTTP 401)"
                429 -> "Превышен лимит запросов к нейросети (HTTP 429)"
                else -> "Ошибка сети или API (HTTP $code)"
            }
        }
    }
}
