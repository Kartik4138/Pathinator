package com.example.pathinator.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pathinator.MainActivity
import com.example.pathinator.client.RetrofitClient
import com.example.pathinator.databinding.ActivityLoginBinding
import com.example.pathinator.interfaces.AuthRequest
import kotlinx.coroutines.launch
import androidx.core.content.edit

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val refreshToken = prefs.getString("refresh_token", null)

        if (!refreshToken.isNullOrEmpty()) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.refreshToken(
                        mapOf("refresh_token" to refreshToken)
                    )
                    if (response.isSuccessful) {
                        val token = response.body()
                        token?.let {
                            prefs.edit {
                                putString("token", it.access_token)
                                putString("refresh_token", it.refresh_token)
                            }
                        }
                        startActivity(Intent(this@Login, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@Login, "Session expired, please login", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@Login, "Error refreshing token", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(username, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }
    }

    private fun loginUser(username: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(AuthRequest(username, password))
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
                        prefs.edit {
                            putString("token", it.access_token)
                            putString("refresh_token", it.refresh_token)
                        }
                        Toast.makeText(this@Login, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@Login, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this@Login, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Login, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
