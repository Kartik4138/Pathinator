package com.example.pathinator.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pathinator.R
import com.example.pathinator.client.RetrofitClient
import com.example.pathinator.databinding.ActivitySignupBinding
import com.example.pathinator.interfaces.AuthRequest
import kotlinx.coroutines.launch

class Signup : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if(username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            } else{
                signupUser(username, password)
            }
        }
    }

    private fun signupUser(username: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.signup(AuthRequest(username, password))
                if (response.isSuccessful) {
                    Toast.makeText(this@Signup, "Signup successful", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@Signup, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Signup, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

