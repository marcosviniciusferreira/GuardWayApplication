package com.example.guardwayapplication

import ApiService // Certifique-se de que ApiService e os modelos estão aqui
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ⚠️ Se RelatorioSegurancaResponse e OcorrenciaItem não estão em ApiService.kt, copie-os para cá
// ou garanta que estejam no escopo de importação.
// Vamos assumir aqui que você já os definiu.
data class OcorrenciaItem( // Exemplo básico, ajuste conforme seu ApiService
    val id_ocorrencia: Int,
    val tipo_ocorrencia: String,
    val endereco: String?,
    val data: String // Data da ocorrência
)

data class RelatorioSegurancaResponse(
    val roubosCarroCount: Int,
    val roubosCelularCount: Int,
    val assaltosCount: Int,
    val atividadeSuspeitaCount: Int,
    val totalOcorrencias: Int,
    val ocorrenciasRecentes: List<OcorrenciaItem>
)

class RelatorioSegurancaActivity : AppCompatActivity() {

    // Constante para o limite de risco (pode ser ajustada)
    private val RISCO_ALTO_THRESHOLD = 50
    private val BASE_URL = "http://192.168.1.4/" // ⚠️ Use a URL base correta

    // UI Components
    private lateinit var tvEnderecoRelatorio: TextView
    private lateinit var tvEstadoRelatorio: TextView
    private lateinit var tvNivelRisco: TextView
    private lateinit var tvRoubosCarro: TextView
    private lateinit var tvRoubosCelular: TextView
    private lateinit var tvAssaltos: TextView
    private lateinit var tvAtividadeSuspeita: TextView
    private lateinit var llOcorrenciasRecentes: LinearLayout // Container para a lista
    private lateinit var btnBack: ImageButton

    // Dados injetados
    private var currentCEP: String? = null
    private var currentFullAddress: String? = null
    private var addressCityState: String = ""

    // Service
    private lateinit var apiService: ApiService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_relatorio_seguranca)

        initViews()
        initializeRetrofit()

        currentFullAddress = intent.getStringExtra("endereco")
        currentCEP = intent.getStringExtra("cep")

        setupAddressDisplay()

        // Inicia a busca real
        fetchSecurityReport()
    }

    private fun initializeRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    private fun initViews() {
        tvEnderecoRelatorio = findViewById(R.id.tv_endereco_relatorio)
        tvEstadoRelatorio = findViewById(R.id.tv_estado_relatorio)
        tvNivelRisco = findViewById(R.id.tv_nivel_risco)
        tvRoubosCarro = findViewById(R.id.tv_roubos_carro)
        tvRoubosCelular = findViewById(R.id.tv_roubos_celular)
        tvAssaltos = findViewById(R.id.tv_assaltos)
        tvAtividadeSuspeita = findViewById(R.id.tv_atividade_suspeita)
        llOcorrenciasRecentes = findViewById(R.id.ll_ocorrencias_recentes)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupAddressDisplay() {
        if (currentFullAddress != null) {
            val parts = currentFullAddress!!.split(",").map { it.trim() }

            if (parts.size >= 2) {
                tvEnderecoRelatorio.text = parts[0]
                val cityStateGuess = if (parts.size >= 3) "${parts[1]}, ${parts[2]}" else parts[1]
                tvEstadoRelatorio.text = cityStateGuess
                addressCityState = cityStateGuess
            } else {
                tvEnderecoRelatorio.text = currentFullAddress
                tvEstadoRelatorio.text = "Localização Desconhecida"
            }
        } else {
            tvEnderecoRelatorio.text = "Endereço Não Encontrado"
            tvEstadoRelatorio.text = "CEP: ${currentCEP ?: "N/A"}"
        }

        clearStatisticViews()
    }

    private fun clearStatisticViews() {
        tvNivelRisco.text = "Carregando..."
        tvRoubosCarro.text = "..."
        tvRoubosCelular.text = "..."
        tvAssaltos.text = "..."
        tvAtividadeSuspeita.text = "..."
        llOcorrenciasRecentes.removeAllViews() // Limpa os includes estáticos
        llOcorrenciasRecentes.visibility = View.GONE
    }

    /**
     * Faz a chamada real à API para buscar o relatório de segurança por CEP.
     */
    private fun fetchSecurityReport() {
        val cep = currentCEP
        if (cep == null) {
            Toast.makeText(this, "CEP não disponível para gerar relatório.", Toast.LENGTH_LONG).show()
            setRiskLevel("Indisponível", android.R.color.darker_gray)
            return
        }

        Log.d("RelatorioSeguranca", "Buscando relatório para o CEP: $cep")
        tvNivelRisco.text = "Avaliando..."

        // ⚠️ CHAMADA RETROFIT REAL
        apiService.getRelatorioSeguranca(cep)
            .enqueue(object : Callback<RelatorioSegurancaResponse> {
                override fun onResponse(
                    call: Call<RelatorioSegurancaResponse>,
                    response: Response<RelatorioSegurancaResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val report = response.body()!!
                        updateReportUI(report)
                    } else {
                        onFailure(call, Exception("Resposta não sucedida: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<RelatorioSegurancaResponse>, t: Throwable) {
                    Log.e("RelatorioSeguranca", "Falha na API: ${t.message}")
                    Toast.makeText(
                        this@RelatorioSegurancaActivity,
                        "Erro de conexão ao buscar relatório.",
                        Toast.LENGTH_SHORT
                    ).show()
                    setRiskLevel("Erro de Rede", android.R.color.darker_gray)
                    tvRoubosCarro.text = "N/A"
                    // ... (outros textos N/A)
                }
            })
    }

    /**
     * Atualiza todos os componentes da UI com os dados do relatório.
     */
    private fun updateReportUI(report: RelatorioSegurancaResponse) {
        // 1. Atualiza as estatísticas
        tvRoubosCarro.text = report.roubosCarroCount.toString()
        tvRoubosCelular.text = report.roubosCelularCount.toString()
        tvAssaltos.text = report.assaltosCount.toString()
        tvAtividadeSuspeita.text = report.atividadeSuspeitaCount.toString()

        // 2. Calcula e define o Nível de Risco
        val total = report.totalOcorrencias
        val riskText = if (total > RISCO_ALTO_THRESHOLD) "ALTO" else "BAIXO"

        val colorResId = when {
            total > RISCO_ALTO_THRESHOLD -> R.color.black // Usando a cor #9F2220 do layout
            total > RISCO_ALTO_THRESHOLD / 2 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_green_dark
        }

        // ⚠️ Nota: Como seu XML usa a cor sólida #9F2220, vamos usá-la como referência.
        // Se R.color.red_guardway não existe, use ContextCompat.getColor(this, R.color.SEU_VERMELHO)
        setRiskLevel(riskText, colorResId)


        // 3. Atualiza as ocorrências recentes
        // Remove os includes estáticos e preenche dinamicamente (IMPLEMENTAÇÃO BÁSICA)
        llOcorrenciasRecentes.removeAllViews()
        if (report.ocorrenciasRecentes.isNotEmpty()) {
            llOcorrenciasRecentes.visibility = View.VISIBLE
            report.ocorrenciasRecentes.forEach { item ->
                // ⚠️ A forma ideal é inflar "item_ocorrencia_recente.xml"
                // Para manter a implementação simples por agora, vamos adicionar um TextView
                val tv = TextView(this).apply {
                    text = "• ${item.tipo_ocorrencia} em ${item.data}"
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    setPadding(0, 8, 0, 8)
                }
                llOcorrenciasRecentes.addView(tv)
            }
        } else {
            llOcorrenciasRecentes.visibility = View.VISIBLE
            val tv = TextView(this).apply {
                text = "Nenhuma ocorrência registrada recentemente pela comunidade."
                setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                setPadding(0, 8, 0, 8)
            }
            llOcorrenciasRecentes.addView(tv)
        }
    }

    /**
     * Define o texto e a cor de fundo do cartão de Nível de Risco.
     */
    private fun setRiskLevel(text: String, colorResId: Int) {
        tvNivelRisco.text = text

        // Encontra o CardView pai para mudar a cor de fundo
        val riskCard = findViewById<View>(R.id.tv_nivel_risco).parent.parent as? androidx.cardview.widget.CardView
        val riskLinearLayout = findViewById<View>(R.id.tv_nivel_risco).parent as? LinearLayout

        // Tenta aplicar a cor ao LinearLayout interno que tem o background="#9F2220"
        if (riskLinearLayout != null) {
            try {
                // Tenta usar a cor #9F2220 como base para cor primária, mas ajusta dinamicamente
                val color = ContextCompat.getColor(this, colorResId)
                riskLinearLayout.setBackgroundColor(color)
            } catch (e: Exception) {
                // Caso a cor não exista no R.color, usa o padrão do Android
                val color = ContextCompat.getColor(this, android.R.color.holo_red_dark)
                riskLinearLayout.setBackgroundColor(color)
            }
        }
    }
}