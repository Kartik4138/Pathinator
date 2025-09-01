package com.example.pathinator

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pathinator.adapter.PathAdapter
import com.example.pathinator.adapter.Session
import com.example.pathinator.client.RetrofitClient
import com.example.pathinator.databinding.ActivitySearchBinding
import com.example.pathinator.interfaces.SessionResponse
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private val sessions = mutableListOf<Session>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvSearch.layoutManager = LinearLayoutManager(this)
        val adapter = PathAdapter(sessions) { session ->
            val intent = Intent(this, Map::class.java)
            intent.putExtra("SESSION_NAME", session.name)
            intent.putExtra("IS_NEW_SESSION", false)
            startActivity(intent)
        }
        binding.rvSearch.adapter = adapter

        fetchSessions(adapter)
    }

    private fun fetchSessions(adapter: PathAdapter) {
        lifecycleScope.launch {
            try {
                val token = getSharedPreferences("prefs", MODE_PRIVATE).getString("token", "")
                val response = RetrofitClient.api.getSessions("Bearer $token")
                if (response.isSuccessful) {
                    val data: List<SessionResponse> = response.body()?: emptyList()
                    sessions.clear()
                    sessions.addAll(data.map { Session(it.name, it.created_at) })
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@SearchActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SearchActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}