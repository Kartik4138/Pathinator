package com.example.pathinator

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pathinator.auth.Login
import com.example.pathinator.client.RetrofitClient
import com.example.pathinator.databinding.ActivityMainBinding
import com.example.pathinator.sessionmanager.SessionManager
import kotlinx.coroutines.launch
import android.Manifest
import android.location.LocationManager
import android.provider.Settings



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_SHORT).show()
                // You can start location updates here
            } else {
                Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkLocationPermission()
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = prefs.getString("token", null)

        val sessionManager = SessionManager(this)
        sessionManager.scheduleTokenRefresh()

        if(token.isNullOrEmpty()) {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        binding.btnStartSession.setOnClickListener {
            val sessionName = binding.etPathName.text.toString()
            if (sessionName.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, Map::class.java)
                intent.putExtra("SESSION_NAME", sessionName)
                intent.putExtra("IS_NEW_SESSION", true)
                startActivity(intent)
            }
        }

        binding.btnLogout.setOnClickListener {
            val token = prefs.getString("token", null)
            if (!token.isNullOrEmpty()) {
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.logout("Bearer $token")
                        if (response.isSuccessful) {
                            prefs.edit().remove("token").apply()
                            prefs.edit().remove("refresh_token").apply()
                            Toast.makeText(this@MainActivity, "Logout successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MainActivity, Login::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to logout", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        binding.ivSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                if (isLocationEnabled()) {
                    Toast.makeText(this, "Permission granted & GPS is ON", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enable GPS/location services", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(this, "Location permission is required for this feature", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

}