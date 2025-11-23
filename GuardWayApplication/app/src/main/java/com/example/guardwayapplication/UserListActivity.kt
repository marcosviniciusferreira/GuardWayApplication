package com.example.guardwayapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserListActivity : AppCompatActivity(), OnUserActionsListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var apiService: LoginActivity.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        recyclerView = findViewById(R.id.recyclerViewUsuarios)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.15/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(LoginActivity.ApiService::class.java)

        loadUsers()
    }

    private fun loadUsers() {
        apiService.getUsuarios().enqueue(object : Callback<List<Usuario>> {
            override fun onResponse(call: Call<List<Usuario>>, response: Response<List<Usuario>>) {
                if (response.isSuccessful) {
                    val usuarios = response.body()?.toMutableList() ?: mutableListOf()
                    userAdapter = UserAdapter(usuarios, this@UserListActivity)
                    recyclerView.adapter = userAdapter
                } else {
                    Log.e("API Error", "Erro ao buscar usuários. Código: ${response.code()}")
                    Toast.makeText(this@UserListActivity, "Erro ao carregar usuários", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Usuario>>, t: Throwable) {
                Log.e("API Failure", "Falha de conexão: ${t.message}", t)
                Toast.makeText(this@UserListActivity, "Falha de conexão", Toast.LENGTH_SHORT).show()
            }
        })
    }



    override fun onUserDelete(userId: Int, position: Int) {
        apiService.deleteUsuario(userId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    userAdapter.removeUser(position)
                    Toast.makeText(this@UserListActivity, "Usuário excluído com sucesso", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("API Error", "Erro ao excluir usuário. Código: ${response.code()}")
                    Toast.makeText(this@UserListActivity, "Erro ao excluir usuário", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API Failure", "Falha de conexão ao excluir: ${t.message}", t)
                Toast.makeText(this@UserListActivity, "Falha de conexão", Toast.LENGTH_SHORT).show()
            }
        })
    }
}