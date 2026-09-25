package com.bloomee.app.assistant

import com.bloomee.app.domain.advice.AdviceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional Gemini-backed chat. The key belongs to the user and is never bundled with the app,
 * so the assistant stays off until a key is entered in settings.
 */
class AssistantClient(
    private val client: OkHttpClient = defaultClient
) {

    suspend fun ask(apiKey: String, contextBlock: String, question: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Asistan için Ayarlar'dan kendi Gemini API anahtarını girmen gerekiyor.")
                )
            }

            val body = JSONObject().apply {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION))
                    )
                )
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put("text", "$contextBlock\n\nSoru: $question")
                                )
                            )
                    )
                )
            }

            val request = Request.Builder()
                .url("$ENDPOINT?key=$apiKey")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            runCatching {
                client.newCall(request).execute().use { response ->
                    val payload = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error(
                            when (response.code) {
                                400, 401, 403 -> "API anahtarı geçersiz veya yetkisiz (kod ${response.code})."
                                429 -> "Gemini kota sınırına takıldı, biraz sonra tekrar dene."
                                else -> "Asistan yanıt veremedi (kod ${response.code})."
                            }
                        )
                    }
                    val text = JSONObject(payload)
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        .orEmpty()
                    if (text.isBlank()) error("Asistandan boş yanıt geldi.")
                    withDisclaimer(text)
                }
            }
        }

    private fun withDisclaimer(text: String): String =
        if (text.contains("tıbbi tavsiye")) text else "$text\n\n${AdviceEngine.MEDICAL_DISCLAIMER}"

    private companion object {
        const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val SYSTEM_INSTRUCTION = """
            Bloomee adlı kadın sağlığı uygulamasının asistanısın. Türkçe, sıcak, kısa ve
            bilimsel olarak doğru yanıtlar verirsin. Teşhis koymaz, ilaç önermezsin.
            Kendini insan gibi tanıtmaz, kendi döngün olduğunu iddia etmezsin.
            Her yanıtın sonuna şu notu eklersin: ${AdviceEngine.MEDICAL_DISCLAIMER}
        """.trimIndent()

        val defaultClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}
