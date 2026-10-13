package com.rekixo.ludopaws

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal data class RemoteForfeitSnapshot(
    val status: String,
    val winnerPlayerId: String?,
)

internal object RemoteForfeitApi {
    fun forfeit(
        matchId: String,
        playerToken: String,
        baseUrl: String =
            BuildConfig.LUDOPROOF_API_BASE_URL,
    ): RemoteForfeitSnapshot =
        request(
            method = "POST",
            matchId = matchId,
            playerToken = playerToken,
            baseUrl = baseUrl,
            body = JSONObject().put("forfeit", true),
        )

    fun state(
        matchId: String,
        playerToken: String,
        baseUrl: String =
            BuildConfig.LUDOPROOF_API_BASE_URL,
    ): RemoteForfeitSnapshot =
        request(
            method = "GET",
            matchId = matchId,
            playerToken = playerToken,
            baseUrl = baseUrl,
            body = null,
        )

    private fun request(
        method: String,
        matchId: String,
        playerToken: String,
        baseUrl: String,
        body: JSONObject?,
    ): RemoteForfeitSnapshot {
        require(
            baseUrl.startsWith(
                "https://",
            ),
        ) {
            "LudoProof API must use HTTPS"
        }

        val normalizedBase =
            baseUrl.trimEnd('/')
        val normalizedMatchId =
            matchId
                .trim()
                .uppercase()
        val connection =
            URL(
                normalizedBase +
                    "/api/matches/" +
                    normalizedMatchId +
                    if (method == "GET") "/state" else "/start",
            )
                .openConnection() as
                HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout =
                3_500
            connection.readTimeout =
                5_000
            connection.instanceFollowRedirects =
                false
            connection.setRequestProperty(
                "Accept",
                "application/json",
            )
            connection.setRequestProperty(
                "Authorization",
                "Bearer " +
                    playerToken,
            )
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json",
                )
            }
            connection.setRequestProperty(
                "User-Agent",
                "LudoProof-Android/" +
                    BuildConfig.VERSION_NAME,
            )

            if (body != null) {
                connection.outputStream.use {
                    it.write(
                        body.toString().toByteArray(Charsets.UTF_8),
                    )
                }
            }

            val status =
                connection.responseCode
            val stream =
                if (
                    status in
                    200..299
                ) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            val responseText =
                stream
                    ?.use {
                        readUtf8Limited(
                            it,
                            MAX_RESPONSE_BYTES,
                        )
                    }
                    .orEmpty()

            if (
                status !in
                200..299
            ) {
                val value =
                    runCatching {
                        JSONObject(
                            responseText,
                        )
                    }
                        .getOrNull()
                throw GameApiException(
                    code =
                        value
                            ?.optString(
                                "error",
                                "HTTP_$status",
                            )
                            ?: "HTTP_$status",
                    message =
                        value
                            ?.optString(
                                "message",
                                "Forfeit failed with HTTP $status",
                            )
                            ?: "Forfeit failed with HTTP $status",
                )
            }
            val payload =
                if (responseText.isBlank()) {
                    JSONObject()
                } else {
                    JSONObject(responseText)
                }
            val state =
                payload.optJSONObject("state")
                    ?: throw GameApiException(
                        code = "INVALID_FORFEIT_RESPONSE",
                        message = "Server response did not include authoritative match state.",
                    )
            val matchStatus =
                state.optString("status")
            if (matchStatus.isBlank()) {
                throw GameApiException(
                    code = "INVALID_FORFEIT_RESPONSE",
                    message = "Server response did not include match status.",
                )
            }
            return RemoteForfeitSnapshot(
                status = matchStatus,
                winnerPlayerId =
                    state.optString("winnerPlayerId")
                        .takeIf(String::isNotBlank),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun readUtf8Limited(
        stream: InputStream,
        maxBytes: Int,
    ): String {
        val output =
            ByteArrayOutputStream()
        val buffer =
            ByteArray(
                4 *
                    1024,
            )
        var total =
            0

        while (
            true
        ) {
            val count =
                stream.read(
                    buffer,
                )
            if (
                count <
                0
            ) {
                break
            }
            total +=
                count
            if (
                total >
                maxBytes
            ) {
                throw GameApiException(
                    code =
                        "RESPONSE_TOO_LARGE",
                    message =
                        "Forfeit response exceeded the safety limit.",
                )
            }
            output.write(
                buffer,
                0,
                count,
            )
        }

        return output
            .toByteArray()
            .toString(
                Charsets.UTF_8,
            )
    }

    private const val MAX_RESPONSE_BYTES =
        64 *
            1024
}
