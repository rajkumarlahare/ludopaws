package com.rekixo.ludopaws

import com.rekixo.ludopaws.feature.characters.data.local.LudoPawsCharacterRuntime
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

class GameApi(
    baseUrl: String = BuildConfig.LUDOPROOF_API_BASE_URL,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    fun registerFriend(displayName: String, clientRequestId: String): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/register",
            body = JSONObject()
                .put("displayName", displayName)
                .put("clientRequestId", clientRequestId),
        )

    fun friendSnapshot(friendToken: String): JSONObject =
        request(
            method = "GET",
            path = "/api/friends/snapshot",
            friendToken = friendToken,
        )

    fun friendHeartbeat(friendToken: String, displayName: String): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/heartbeat",
            friendToken = friendToken,
            body = JSONObject().put("displayName", displayName),
        )

    fun sendFriendRequest(friendToken: String, friendId: String): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/request",
            friendToken = friendToken,
            body = JSONObject().put("friendId", friendId),
        )

    fun respondFriendRequest(friendToken: String, requestId: String, accept: Boolean): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/request/respond",
            friendToken = friendToken,
            body = JSONObject()
                .put("requestId", requestId)
                .put("accept", accept),
        )

    fun removeFriend(friendToken: String, friendId: String): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/remove",
            friendToken = friendToken,
            body = JSONObject().put("friendId", friendId),
        )

    fun sendFriendInvite(
        friendToken: String,
        friendId: String,
        matchId: String,
        roomPlayerToken: String,
        clientRequestId: String,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/invite",
            friendToken = friendToken,
            roomPlayerToken = roomPlayerToken,
            body = JSONObject()
                .put("friendId", friendId)
                .put("matchId", matchId)
                .put("clientRequestId", clientRequestId),
        )

    fun respondFriendInvite(
        friendToken: String,
        inviteId: String,
        accept: Boolean,
        clientRequestId: String? = null,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/invite/respond",
            friendToken = friendToken,
            body = JSONObject()
                .put("inviteId", inviteId)
                .put("accept", accept)
                .apply {
                    if (accept) {
                        require(!clientRequestId.isNullOrBlank()) {
                            "clientRequestId is required when accepting a friend invite"
                        }
                        put("clientRequestId", clientRequestId)
                    }
                },
        )

    fun sendFriendMessage(
        friendToken: String,
        friendId: String,
        text: String,
        clientRequestId: String,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/messages/send",
            friendToken = friendToken,
            body = JSONObject()
                .put("friendId", friendId)
                .put("text", text)
                .put("clientRequestId", clientRequestId),
        )

    fun friendMessages(friendToken: String, friendId: String): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/messages/list",
            friendToken = friendToken,
            body = JSONObject().put("friendId", friendId),
        )

    fun friendRecent(friendToken: String): JSONObject =
        request(
            method = "POST",
            path = "/api/friends/recent",
            friendToken = friendToken,
            body = JSONObject(),
        )

    fun createFriendRoom(
        friendToken: String,
        displayName: String,
        clientRequestId: String,
        playerCount: Int,
        characterId: String = LudoPawsCharacterRuntime.selectedCharacterId(),
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches",
            friendToken = friendToken,
            body = JSONObject()
                .put("displayName", displayName)
                .put("clientRequestId", clientRequestId)
                .put("characterId", characterId)
                .put("targetPlayerCount", playerCount)
                .put("matchMode", "FRIENDS")
                .put("rulesetKey", "CLASSIC_V4"),
        )

    fun searchPublicMatch(
        profileToken: String,
        displayName: String,
        clientRequestId: String,
        playerCount: Int,
        characterId: String = LudoPawsCharacterRuntime.selectedCharacterId(),
        rulesetKey: String = "CLASSIC_V4",
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matchmaking/search",
            profileToken = profileToken,
            body = JSONObject()
                .put("displayName", displayName)
                .put("clientRequestId", clientRequestId)
                .put("characterId", characterId)
                .put("playerCount", playerCount)
                .put("rulesetKey", rulesetKey),
        )

    fun publicMatchStatus(
        profileToken: String,
        clientRequestId: String,
        playerCount: Int,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matchmaking/status",
            profileToken = profileToken,
            body = JSONObject()
                .put("clientRequestId", clientRequestId)
                .put("playerCount", playerCount),
        )

    fun cancelPublicMatch(
        profileToken: String,
        clientRequestId: String,
        playerCount: Int,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matchmaking/cancel",
            profileToken = profileToken,
            body = JSONObject()
                .put("clientRequestId", clientRequestId)
                .put("playerCount", playerCount),
        )

    fun createMatch(
        profileToken: String,
        displayName: String,
        clientRequestId: String,
        characterId: String = LudoPawsCharacterRuntime.selectedCharacterId(),
        rulesetKey: String = "CLASSIC_V4",
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches",
            profileToken = profileToken,
            body = JSONObject()
                .put("displayName", displayName)
                .put("clientRequestId", clientRequestId)
                .put("characterId", characterId)
                .put("rulesetKey", rulesetKey),
        )

    fun joinMatch(
        matchId: String,
        displayName: String,
        clientRequestId: String,
        profileToken: String? = null,
        friendJoinToken: String? = null,
        characterId: String = LudoPawsCharacterRuntime.selectedCharacterId(),
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/join",
            profileToken = profileToken,
            friendJoinToken = friendJoinToken,
            body = JSONObject()
                .put("displayName", displayName)
                .put("clientRequestId", clientRequestId)
                .put("characterId", characterId),
        )

    fun state(matchId: String, playerToken: String): JSONObject =
        request(
            method = "GET",
            path = "/api/matches/${matchId.uppercase()}/state",
            playerToken = playerToken,
        )

    fun start(matchId: String, playerToken: String): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/start",
            playerToken = playerToken,
            body = JSONObject(),
        )

    fun roll(
        matchId: String,
        playerToken: String,
        clientSeed: String,
        clientCommitment: String,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/roll",
            playerToken = playerToken,
            body = JSONObject()
                .put("clientSeed", clientSeed)
                .put("clientCommitment", clientCommitment),
        )

    fun move(
        matchId: String,
        playerToken: String,
        tokenIndex: Int,
        eventIndex: Int,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/move",
            playerToken = playerToken,
            body = JSONObject()
                .put("tokenIndex", tokenIndex)
                .put("eventIndex", eventIndex),
        )

    private fun request(
        method: String,
        path: String,
        playerToken: String? = null,
        friendToken: String? = null,
        profileToken: String? = null,
        roomPlayerToken: String? = null,
        friendJoinToken: String? = null,
        body: JSONObject? = null,
    ): JSONObject {
        require(baseUrl.startsWith("https://")) {
            "LudoProof API must use HTTPS"
        }
        val requestBuilder = Request.Builder()
            .url(baseUrl + path)
            .header("Accept", "application/json")
            .header(
                "User-Agent",
                "LudoProof-Android/" + BuildConfig.VERSION_NAME,
            )

        val bearerToken = playerToken ?: friendToken ?: profileToken
        if (bearerToken != null) {
            requestBuilder.header("Authorization", "Bearer $bearerToken")
        }
        if (roomPlayerToken != null) {
            requestBuilder.header("X-LudoProof-Room-Token", roomPlayerToken)
        }
        if (friendJoinToken != null) {
            requestBuilder.header("X-LudoProof-Friend-Join-Token", friendJoinToken)
        }

        val requestBody = when {
            body != null -> body.toString().toByteArray(Charsets.UTF_8)
                .toRequestBody(JSON_MEDIA_TYPE)
            method != "GET" && method != "HEAD" ->
                ByteArray(0).toRequestBody(JSON_MEDIA_TYPE)
            else -> null
        }
        if (body != null || requestBody != null) {
            requestBuilder.header("Content-Type", "application/json")
        }

        val request = requestBuilder.method(method, requestBody).build()
        LudoPawsHttpClients.shared.newCall(request).execute().use { response ->
            val status = response.code
            val text = response.body
                ?.byteStream()
                ?.use { readUtf8Limited(it, MAX_RESPONSE_BYTES) }
                .orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) {
                throw GameApiException(
                    code = json.optString("error", "HTTP_$status"),
                    message = json.optString("message", "Request failed with HTTP $status"),
                )
            }
            return json
        }
    }

    private fun readUtf8Limited(stream: InputStream, maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) {
                throw GameApiException(
                    code = "RESPONSE_TOO_LARGE",
                    message = "Server response exceeded the safety limit.",
                )
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray().toString(Charsets.UTF_8)
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 512 * 1024
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    }
}

class GameApiException(
    val code: String,
    override val message: String,
) : RuntimeException(message)
