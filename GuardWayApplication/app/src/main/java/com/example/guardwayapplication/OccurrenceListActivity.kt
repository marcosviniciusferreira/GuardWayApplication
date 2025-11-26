package com.example.guardwayapplication

import ApiService
import Ocorrencia
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Interface para definir as ações de callback do Adapter
interface OnOccurrenceActionsListener {
    fun onOccurrenceDelete(ocorrenciaId: Int, position: Int)
    fun onOccurrenceEdit(ocorrencia: Ocorrencia)
}

class OccurrenceListActivity : AppCompatActivity(), OnOccurrenceActionsListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var occurrenceAdapter: OccurrenceAdapter
    private lateinit var btnAddOccurrence: FloatingActionButton

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usa o novo layout activity_occurrence_list.xml
        setContentView(R.layout.activity_occurrence_list)

        recyclerView = findViewById(R.id.recyclerViewOcorrencias)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnAddOccurrence = findViewById(R.id.btnAddOccurrence)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.15/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        loadOcorrencias()

        btnAddOccurrence.setOnClickListener {
            val intent = Intent(this, OccurrenceFormActivity::class.java)
            formLauncher.launch(intent)
        }
    }

    private fun loadOcorrencias() {
        apiService.getOcorrencias().enqueue(object : Callback<List<Ocorrencia>> {
            override fun onResponse(call: Call<List<Ocorrencia>>, response: Response<List<Ocorrencia>>) {
                if (response.isSuccessful) {
                    val ocorrencias = response.body()?.toMutableList() ?: mutableListOf()
                    occurrenceAdapter = OccurrenceAdapter(ocorrencias, this@OccurrenceListActivity)
                    recyclerView.adapter = occurrenceAdapter
                } else {
                    Log.e("API Error", "Erro ao buscar ocorrências. Código: ${response.code()}")
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
        intent.putExtra("OCORRENCIA_EXTRA", ocorrencia)
        formLauncher.launch(intent)
    }

    // Lançador de Activity para recarregar a lista após adicionar/editar
    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadOcorrencias()
        }
    }
}