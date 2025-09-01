package com.example.pathinator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.pathinator.client.RetrofitClient
import com.example.pathinator.databinding.ActivityMapBinding
import com.example.pathinator.interfaces.PathPointRequest
import com.example.pathinator.interfaces.SessionCreateRequest
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.net.URLEncoder

class Map : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private val pathPoints = mutableListOf<LatLng>()
    private var isNewSession = true
    private var sessionName: String? = null
    private var latestLocation: Location? = null


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        sessionName = intent.getStringExtra("SESSION_NAME")
        isNewSession = intent.getBooleanExtra("IS_NEW_SESSION", true)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        binding.btnStopSession.setOnClickListener {
            stopSession()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        if (isNewSession) startNewSession() else loadExistingSession()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startNewSession() {
        if (sessionName.isNullOrEmpty()) sessionName = "New Session"

        lifecycleScope.launch {
            safeApiCall {
                val token = getAccessToken() ?: return@safeApiCall
                RetrofitClient.api.createSession("Bearer $token", SessionCreateRequest(sessionName!!))
            }
        }

        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        serviceIntent.putExtra(LocationTrackingService.EXTRA_SESSION_NAME, sessionName)
        startForegroundService(serviceIntent)

        var isFirstLocation = true

        trackLocation { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            pathPoints.add(latLng)
            map.addPolyline(
                PolylineOptions().addAll(pathPoints).width(10f).color(0xFF007bFF.toInt())
            )
            if (isFirstLocation) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                isFirstLocation = false
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                latestLocation?.let { location ->
                    safeApiCall {
                        val token = getAccessToken() ?: return@safeApiCall
                        RetrofitClient.api.addPathPoint(
                            "Bearer $token",
                            sessionName!!,
                            PathPointRequest(location.latitude, location.longitude)
                        )
                    }
                }
                kotlinx.coroutines.delay(5000)
            }
        }

    }


    private fun loadExistingSession() {
        lifecycleScope.launch {
            safeApiCall {
                Toast.makeText(this@Map ,"Loading session: $sessionName", Toast.LENGTH_SHORT).show()
                val token = getAccessToken() ?: return@safeApiCall
                val response = RetrofitClient.api.getSessionPath("Bearer $token", sessionName!!)
                if (response.isSuccessful) {
                    val points = response.body() ?: emptyList()
                    pathPoints.clear()
                    pathPoints.addAll(points.map { LatLng(it.latitude, it.longitude) })
                    map.addPolyline(PolylineOptions().addAll(pathPoints).width(10f).color(0xFF007bFF.toInt()))
                    if (pathPoints.isNotEmpty()) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pathPoints.last(), 17f))
                    }
                }
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun trackLocation(onUpdate: (Location) -> Unit) {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc ->
                    latestLocation = loc
                    onUpdate(loc) }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L) // every 2 sec
            .setMinUpdateDistanceMeters(1f)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }


    private fun stopSession() {
        lifecycleScope.launch {
            safeApiCall {
                val token = getAccessToken() ?: return@safeApiCall
                RetrofitClient.api.stopSession("Bearer $token")
            }
            stopService(Intent(this@Map, LocationTrackingService::class.java))
            Toast.makeText(this@Map, "Session stopped", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    private fun getAccessToken(): String? {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        return prefs.getString("token", null)
    }

    private suspend fun refreshAccessToken(): String? {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val refreshToken = prefs.getString("refresh_token", null) ?: return null

        return try {
            val response = RetrofitClient.api.refreshToken(mapOf("refresh_token" to refreshToken))
            if (response.isSuccessful) {
                val newToken = response.body()
                prefs.edit().putString("token", newToken?.access_token).apply()
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

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { binding.mapView.onPause(); super.onPause() }
    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        binding.mapView.onDestroy()
        super.onDestroy()
    }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (isNewSession) startNewSession() else loadExistingSession()
        }
    }
}
