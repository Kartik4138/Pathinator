package com.example.pathinator

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.pathinator.client.RetrofitClient
import com.example.pathinator.interfaces.PathPointRequest
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

class LocationTrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var sessionName: String? = null

    companion object {
        const val CHANNEL_ID = "path_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_SESSION_NAME = "SESSION_NAME"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    sessionName?.let { sendPointToBackend(it, location) }
                }
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        sessionName = intent?.getStringExtra(EXTRA_SESSION_NAME)

        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Path Tracking Active")
            .setContentText("Your path is being recorded")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your app icon
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        startTracking()
        return START_STICKY
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun sendPointToBackend(session: String, location: Location) {
        lifecycleScope.launch {
            safeApiCall {
                val token = getAccessToken() ?: return@safeApiCall
                RetrofitClient.api.addPathPoint(
                    "Bearer $token",
                    session,
                    PathPointRequest(location.latitude, location.longitude)
                )
            }
        }
    }

    // --- Token helpers ---
    private fun getAccessToken(): String? {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        return prefs.getString("access_token", null)
    }

    private suspend fun refreshAccessToken(): String? {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val refreshToken = prefs.getString("refresh_token", null) ?: return null
        return try {
            val response = RetrofitClient.api.refreshToken(mapOf("refresh_token" to refreshToken))
            if (response.isSuccessful) {
                val newToken = response.body()
                prefs.edit().putString("access_token", newToken?.access_token).apply()
                newToken?.access_token
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun safeApiCall(apiCall: suspend () -> Unit) {
        try {
            apiCall()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) { // token expired
                val newToken = refreshAccessToken() ?: return
                apiCall()
            }
        } catch (_: Exception) {}
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Path Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
}
