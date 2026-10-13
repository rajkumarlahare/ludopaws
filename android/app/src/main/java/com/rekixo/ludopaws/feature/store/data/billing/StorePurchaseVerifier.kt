package com.rekixo.ludopaws.feature.store.data.billing

import com.rekixo.ludopaws.BuildConfig
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Public feature-availability probe only. Purchase verification/grants must flow through
 * AccountRepository and the authenticated account-bound server endpoint.
 */
class StorePurchaseVerifier(
    baseUrl: String = BuildConfig.LUDOPROOF_API_BASE_URL,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    fun status(): JSONObject = requestStatus()

    fun isBillingAvailable(): Boolean =
        requestStatus().optBoolean("billingAvailable", false)

    fun isRewardedAdsAvailable(): Boolean =
        requestStatus().optBoolean("rewardedAdsAvailable", false)

    private fun requestStatus(): JSONObject {
        require(baseUrl.startsWith("https://")) { "LudoProof API must use HTTPS" }
        val connection = URL(baseUrl + "/api/store/status").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "LudoProof-Android/" + BuildConfig.VERSION_NAME)
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { readUtf8Limited(it, MAX_RESPONSE_BYTES) }.orEmpty()
            val response = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) {
                throw StorePurchaseVerificationException(
                    response.optString("error").ifBlank { "HTTP_$status" },
                    response.optString("message").ifBlank { "Store status failed with HTTP $status" },
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun readUtf8Limited(stream: InputStream, maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) {
                throw StorePurchaseVerificationException(
                    "RESPONSE_TOO_LARGE",
                    "Store status response exceeded the safety limit.",
                )
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    companion object {
        private const val MAX_RESPONSE_BYTES = 8 * 1024
    }
}

class StorePurchaseVerificationException(
    val code: String?,
    message: String,
) : Exception(message)
