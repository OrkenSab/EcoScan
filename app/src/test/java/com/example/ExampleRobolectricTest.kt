package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.GroqApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EcoScan", appName)
  }

  @Test
  fun `parse groq json with think tags and markdown fences`() {
    val service = GroqApiService()
    val rawResponse = """
      <think>
      Looking at the image, I see multiple plastic bottles thrown on the grass in Kazakhstan.
      Pollution level is medium.
      </think>
      ```json
      {
        "is_trash": true,
        "pollution_score": 5,
        "trash_types": ["пластик", "бутылки"],
        "summary": "На траве обнаружены выброшенные пластиковые бутылки.",
        "harm": {
          "nature": "Вред степной флоре и фауне.",
          "health": "Загрязнение зоны отдыха.",
          "economy": "Затраты на уборку парков."
        },
        "advice": "Соберите пластик в желтый контейнер."
      }
      ```
    """.trimIndent()

    val result = service.extractAndParseJson(rawResponse)
    assertNotNull(result)
    assertTrue(result!!.isTrash)
    assertEquals(5, result.pollutionScore)
    assertEquals(2, result.trashTypes.size)
    assertEquals("пластик", result.trashTypes[0])
    assertEquals("Соберите пластик в желтый контейнер.", result.advice)
  }

  @Test
  fun `parse clean image response where is_trash is false`() {
    val service = GroqApiService()
    val rawResponse = """
      {
        "is_trash": false,
        "pollution_score": 0,
        "trash_types": [],
        "summary": "Чистый горный пейзаж без мусора.",
        "harm": {
          "nature": "Экосистема в порядке.",
          "health": "Воздух и вода чистые.",
          "economy": "Развитие экотуризма."
        },
        "advice": "Продолжайте сохранять природу Казахстана в чистоте!"
      }
    """.trimIndent()

    val result = service.extractAndParseJson(rawResponse)
    assertNotNull(result)
    assertFalse(result!!.isTrash)
    assertEquals(0, result.pollutionScore)
    assertEquals(0, result.trashTypes.size)
  }
}
