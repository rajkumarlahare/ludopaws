package com.ludoproof.game.feature.store.data.billing

import com.ludoproof.game.BuildConfig
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class StorePurchaseVerifier(
    baseUrl: String = BuildConfig.LUDOPROOF_API_BASE_URL,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    fun isBillingAvailable(): Boolean {
        val response =
            request(
                method = "GET",
                path = "/api/store/status",
            )
        return response.optBoolean("billingAvailable", false)
    }

    fun verifyPurchase(
        productId: String,
        purchaseToken: String,
        accountId: String,
    ): Boolean {
        require(productId.isNotBlank())
        require(purchaseToken.isNotBlank())
        require(accountId.matches(ACCOUNT_ID_REGEX))
        val response =
            request(
                method = "POST",
                path = "/api/store/purchases/verify",
                body =
                    JSONObject()
                        .put("productId", productId)
                        .put("purchaseToken", purchaseToken)
                        .put("accountId", accountId),
            )
        return response.optBoolean("ok") &&
            response.optBoolean("verified") &&
            response.optString("productId") == productId
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
    ): JSONObject {
        require(baseUrl.startsWith("https://")) {
            "LudoProof API must use HTTPS"
        }
        val connection =
            URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "LudoProof-Android/" + BuildConfig.VERSION_NAME,
            )
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                require(bytes.size <= MAX_REQUEST_BYTES) {
                    "Store verification request exceeded the safety limit."
                }
                connection.outputStream.use { it.write(bytes) }
            }

            val status = connection.responseCode
            val stream =
                if (status in 200..299) connection.inputStream
                else connection.errorStream
            val text =
                stream?.use { readUtf8Limited(it, MAX_RESPONSE_BYTES) }.orEmpty()
            val response =
                if (text.isBlank()) JSONObject()
                else JSONObject(text)
            if (status !in 200..299) {
                throw StorePurchaseVerificationException(
                    response.optString("error").ifBlank { null },
                    response.optString(
                        "message",
                        "Store request failed with HTTP $status",
                    ),
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun readUtf8Limited(
        stream: InputStream,
        maxBytes: Int,
    ): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4 * 1024)
        var total = 0
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) {
                throw StorePurchaseVerificationException(
                    "RESPONSE_TOO_LARGE",
                    "Store verification response exceeded the safety limit.",
                )
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    companion object {
        private const val MAX_REQUEST_BYTES = 8 * 1024
        private const val MAX_RESPONSE_BYTES = 8 * 1024
        private val ACCOUNT_ID_REGEX = Regex("[a-f0-9]{64}")
    }
}

class StorePurchaseVerificationException(
    val code: String?,
    message: String,
) : Exception(message)
