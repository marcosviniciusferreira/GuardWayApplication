package com.example.guardwayapplication

import Ocorrencia
import ApiService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton // ⭐️ NOVO IMPORT: Use MaterialButton ⭐️
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Interface definida aqui para garantir que o Adapter funcione.
interface OnOccurrenceActionsListener {
    fun onOccurrenceDelete(ocorrenciaId: Int, position: Int)
    fun onOccurrenceEdit(ocorrencia: Ocorrencia)
}

class OccurrenceListActivity : AppCompatActivity(), OnOccurrenceActionsListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var occurrenceAdapter: OccurrenceAdapter

    // ⭐️ CORREÇÃO: Variável deve ser MaterialButton ⭐️
    private lateinit var btnAddOccurrence: MaterialButton

    private lateinit var apiService: ApiService
    private lateinit var sharedPrefsManager: SharedPreferencesManager

    private val BASE_URL = "http://192.168.1.9/"

    // Launcher para iniciar o formulário de ocorrência e esperar pelo resultado
    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadOccurrences() // Recarrega a lista se uma alteração foi feita no formulário
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_occurence_list)

        // Configuração do botão de voltar da Toolbar (ID: btn_back)
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            onBackPressed()
        }

        // Inicialização do SharedPreferencesManager
        sharedPrefsManager = SharedPreferencesManager(this)

        // Inicialização das Views
        recyclerView = findViewById(R.id.recyclerViewOcorrencias)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ⭐️ CORREÇÃO DO CAST: findViewById para MaterialButton ⭐️
        btnAddOccurrence = findViewById(R.id.btn_add)

        // Configuração do Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        loadOccurrences()

        btnAddOccurrence.setOnClickListener {
            val intent = Intent(this, OccurrenceFormActivity::class.java)
            formLauncher.launch(intent)
        }
    }

    private fun loadOccurrences() {
        val userId = sharedPrefsManager.getUserId()

        if (userId == 0) {
            Toast.makeText(this, "Usuário não logado. Faça o login para ver suas ocorrências.", Toast.LENGTH_LONG).show()
            occurrenceAdapter = OccurrenceAdapter(mutableListOf(), this)
            recyclerView.adapter = occurrenceAdapter
            return
        }

        apiService.getOcorrenciasByUserId(userId).enqueue(object : Callback<List<Ocorrencia>> {
            override fun onResponse(call: Call<List<Ocorrencia>>, response: Response<List<Ocorrencia>>) {
                if (response.isSuccessful) {
                    val ocorrencias = response.body()?.toMutableList() ?: mutableListOf()

                    if (ocorrencias.isEmpty()) {
                        Toast.makeText(this@OccurrenceListActivity, "Você não tem ocorrências cadastradas.", Toast.LENGTH_LONG).show()
                    }

                    occurrenceAdapter = OccurrenceAdapter(ocorrencias, this@OccurrenceListActivity)
                    recyclerView.adapter = occurrenceAdapter
                } else {
                    Log.e("API Error", "Erro ao buscar ocorrências. Código: ${response.code()}. URL: ${call.request().url}")
                    Toast.makeText(this@OccurrenceListActivity, "Erro ao carregar ocorrências", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Ocorrencia>>, t: Throwable) {
                Log.e("API Failure", "Falha de conexão: ${t.message}", t)
                Toast.makeText(this@OccurrenceListActivity, "Falha de conexão", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOccurrenceDelete(ocorrenciaId: Int, position: Int) {
        apiService.deleteOcorrencia(ocorrenciaId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    occurrenceAdapter.removeOccurrence(position)
                    Toast.makeText(this@OccurrenceListActivity, "Ocorrência excluída com sucesso", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("API Error", "Erro ao excluir ocorrência. Código: ${response.code()}")
                    Toast.makeText(this@OccurrenceListActivity, "Erro ao excluir ocorrência", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API Failure", "Falha de conexão ao excluir: ${t.message}", t)
                Toast.makeText(this@OccurrenceListActivity, "Falha de conexão", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOccurrenceEdit(ocorrencia: Ocorrencia) {
        val intent = Intent(this, OccurrenceFormActivity::class.java)
        // Passa o objeto Ocorrencia para o formulário de edição
        intent.putExtra("OCORRENCIA_EXTRA", ocorrencia)
        formLauncher.launch(intent)
    }
}