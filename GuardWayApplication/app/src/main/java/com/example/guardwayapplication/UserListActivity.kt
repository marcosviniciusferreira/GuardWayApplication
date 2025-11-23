// src/main/java/com/example/guardwayapplication/UserListActivity.kt
package com.example.guardwayapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list) // Layout para a lista de usuários

        // 1. Configurar o RecyclerView
        recyclerView = findViewById(R.id.recyclerViewUsuarios) //
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 2. Configurar o Retrofit (usando seu IP de login)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.15/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(LoginActivity.ApiService::class.java)
        apiService.getUsuarios().enqueue(object : Callback<List<Usuario>> {
            override fun onResponse(call: Call<List<Usuario>>, response: Response<List<Usuario>>) {
                if (response.isSuccessful) {
                    val usuarios = response.body() ?: emptyList()
                    recyclerView.adapter = UserAdapter(usuarios)
                } else {
                    Log.e("API Error", "Erro ao buscar usuários. Código: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Usuario>>, t: Throwable) {
                Log.e("API Failure", "Falha de conexão: ${t.message}", t)
            }
        })
    }
}