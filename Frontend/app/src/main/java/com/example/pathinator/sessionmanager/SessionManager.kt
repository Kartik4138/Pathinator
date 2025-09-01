package com.example.pathinator.sessionmanager

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import kotlinx.coroutines.*
import retrofit2.Response
import com.example.pathinator.client.RetrofitClient
import com.example.pathinator.interfaces.Token

class SessionManager(private val context: Context) {

    object JwtUtils {
        fun getTokenExpiry(token: String): Long? {
            return try {
                val parts = token.split(".")
                if (parts.size != 3) return null
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                val json = JSONObject(payload)
                json.getLong("exp") * 1000
            } catch (e: Exception) {
                null
            }
        }
    }

    private val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private var refreshJob: Job? = null

    fun scheduleTokenRefresh() {
        val token = prefs.getString("token", null) ?: return
        val refreshToken = prefs.getString("refresh_token", null) ?: return

        val expiry = JwtUtils.getTokenExpiry(token) ?: return
        val now = System.currentTimeMillis()
        val refreshTime = expiry - now - (2 * 60 * 1000)

        if (refreshTime > 0) {
            refreshJob?.cancel()
            refreshJob = CoroutineScope(Dispatchers.IO).launch {
                delay(refreshTime)
                refreshToken(refreshToken)
            }
        }
    }

    private suspend fun refreshToken(refreshToken: String) {
        try {
            val response: Response<Token> = RetrofitClient.api.refreshToken(
                mapOf("refresh_token" to refreshToken)
            )
            if (response.isSuccessful) {
                val newToken = response.body()
                newToken?.let {
                    prefs.edit()
                        .putString("token", it.access_token)
                        .putString("refresh_token", it.refresh_token)
                        .apply()
                    scheduleTokenRefresh()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
