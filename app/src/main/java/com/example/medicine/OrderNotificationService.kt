package com.example.medicine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object OrderNotificationService {
    private const val TAG = "OrderNotificationService"
    // Replace with your backend endpoint that sends SMS + FCM.
    private const val ORDER_NOTIFY_URL = "https://your-backend.example.com/api/notify-order"

    suspend fun notifyOrderPlaced(
        orderId: String,
        phone: String,
        title: String,
        body: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("orderId", orderId)
                put("phone", phone)
                put("title", title)
                put("body", body)
            }

            val connection = (URL(ORDER_NOTIFY_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Notification API failed with code $code")
            }
        }.onFailure {
            Log.e(TAG, "Failed to notify order placement", it)
        }
    }
}
