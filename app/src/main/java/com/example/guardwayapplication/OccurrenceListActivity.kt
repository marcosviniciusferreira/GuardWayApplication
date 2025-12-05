package com.example.guardwayapplication

import Ocorrencia
import ApiService
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

// Interface definida aqui para garantir que o Adapter funcione.
// Recomenda-se mover esta interface para um arquivo Kotlin separado (ex: OnOccurrenceActionsListener.kt).
interface OnOccurrenceActionsListener {
    fun onOccurrenceDelete(ocorrenciaId: Int, position: Int)
    fun onOccurrenceEdit(ocorrencia: Ocorrencia)
}

// A Activity implementa a interface correta
class OccurrenceListActivity : AppCompatActivity(), OnOccurrenceActionsListener {

    private lateinit var recyclerView: RecyclerView
    // Nome da variável de instância corrigido para 'occurrenceAdapter'
    private lateinit var occurrenceAdapter: OccurrenceAdapter
    // ID da variável de instância corrigido para o botão de ocorrências
    private lateinit var btnAddOccurrence: FloatingActionButton

    private lateinit var apiService: ApiService
    private val BASE_URL = "http://192.168.1.9/" // Base URL do seu servidor

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
        // Usando o layout correto para a lista de ocorrências
        setContentView(R.layout.activity_occurence_list)

        // Usando o ID do RecyclerView para ocorrências
        recyclerView = findViewById(R.id.recyclerViewOcorrencias)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Usando o ID do FAB para ocorrências
        btnAddOccurrence = findViewById(R.id.btnAddOcurrence)

        // Configuração do Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        loadOccurrences()

        btnAddOccurrence.setOnClickListener {
            // Abre o formulário de criação de Ocorrência
            val intent = Intent(this, OccurrenceFormActivity::class.java)
            formLauncher.launch(intent)
        }
    }

    private fun loadOccurrences() {
        // Chamando o método correto para buscar ocorrências
        apiService.getOcorrencias().enqueue(object : Callback<List<Ocorrencia>> {
            override fun onResponse(call: Call<List<Ocorrencia>>, response: Response<List<Ocorrencia>>) {
                if (response.isSuccessful) {
                    val ocorrencias = response.body()?.toMutableList() ?: mutableListOf()
                    // Instanciando a classe 'OccurrenceAdapter' com o nome de variável corrigido
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

    // Implementação da ação de exclusão da interface OnOccurrenceActionsListener
    override fun onOccurrenceDelete(ocorrenciaId: Int, position: Int) {
        apiService.deleteOcorrencia(ocorrenciaId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Chamando o método correto no adapter
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

    // Implementação da ação de edição da interface OnOccurrenceActionsListener
    override fun onOccurrenceEdit(ocorrencia: Ocorrencia) {
        val intent = Intent(this, OccurrenceFormActivity::class.java)
        // Passa o objeto Ocorrencia para o formulário de edição
        intent.putExtra("OCORRENCIA_EXTRA", ocorrencia)
        formLauncher.launch(intent)
    }
}