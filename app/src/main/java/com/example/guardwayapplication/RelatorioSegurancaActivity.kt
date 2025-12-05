package com.example.guardwayapplication

import ApiService
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater // Importação necessária para inflar layouts
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
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

class RelatorioSegurancaActivity : AppCompatActivity() {

    // Constante para o limite de risco (pode ser ajustada)
    private val RISCO_ALTO_THRESHOLD = 10
    private val BASE_URL = "http://192.168.1.9/"

    // Array de meses para formatação (0-jan, 11-dez)
    private val nomesMeses = arrayOf(
        "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
        "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
    )

    // UI Components
    private lateinit var tvEnderecoRelatorio: TextView
    private lateinit var tvEstadoRelatorio: TextView
    private lateinit var tvNivelRisco: TextView
    private lateinit var tvFurtoRoubos: TextView
    private lateinit var tvVandalismo: TextView
    private lateinit var tvAssedio: TextView
    private lateinit var tvAtividadeSuspeita: TextView
    private lateinit var llOcorrenciasRecentes: LinearLayout
    private lateinit var llNivelRiscoContainer: LinearLayout // NOVO: Componente para o background do risco
    private lateinit var btnBack: ImageView

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
        tvFurtoRoubos = findViewById(R.id.tv_roubo_furto)
        tvVandalismo = findViewById(R.id.tv_vandalismo)
        tvAssedio = findViewById(R.id.tv_assedio)
        tvAtividadeSuspeita = findViewById(R.id.tv_atividade_suspeita)
        llOcorrenciasRecentes = findViewById(R.id.ll_ocorrencias_recentes)
        llNivelRiscoContainer = findViewById(R.id.ll_nivel_risco_container) // Inicialização do novo componente
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupAddressDisplay() {
        if (currentFullAddress != null) {
            // Tenta separar o endereço completo em partes
            // O formato esperado é: "Rua, Bairro, Cidade, Estado, País"
            // Ou, mais comum para endereço completo: "Rua Exemplo, 123 - Bairro Teste, Cidade, Estado, País"

            val parts = currentFullAddress!!.split(",").map { it.trim() }

            if (parts.size >= 4) {
                // Supondo que as primeiras partes são Endereço + Bairro (ou algo assim),
                // e as duas últimas são Cidade e Estado/País.

                // 1. Pega as partes que formam a linha do endereço (Rua, Número, Bairro)
                // Agrupa todas as partes EXCETO as duas últimas (Cidade, Estado/País)
                // Ex: Se parts é ["Rua X, 123 - Bairro Y", "Cidade Z", "Estado K", "País W"]
                // A parte do endereço será: "Rua X, 123 - Bairro Y" (parts[0])
                val addressParts = parts.subList(0, parts.size - 2).joinToString(", ")

                // 2. Pega as duas últimas partes para formar "Cidade, Estado/País"
                val cityStatePart = "${parts[parts.size - 2]}, ${parts[parts.size - 1]}"

                tvEnderecoRelatorio.text = addressParts
                tvEstadoRelatorio.text = cityStatePart
                addressCityState = cityStatePart

            } else if (parts.size == 3) {
                tvEnderecoRelatorio.text = parts[0] // Endereço (Rua)
                val cityStatePart = "${parts[1]}, ${parts[2]}"
                tvEstadoRelatorio.text = cityStatePart // Cidade, Estado
                addressCityState = cityStatePart

            } else if (parts.isNotEmpty()) {
                // Caso de endereço simples, ex: "Rua Teste"
                tvEnderecoRelatorio.text = currentFullAddress
                tvEstadoRelatorio.text = "Localização Sem Detalhes"
            } else {
                // Fallback
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
        tvFurtoRoubos.text = "..."
        tvVandalismo.text = "..."
        tvAssedio.text = "..."
        tvAtividadeSuspeita.text = "..."
        // Ocultar o contêiner de ocorrências até que os dados sejam carregados
        llOcorrenciasRecentes.removeAllViews()
        llOcorrenciasRecentes.visibility = View.GONE
    }

    private fun fetchSecurityReport() {
        val cep = currentCEP
        if (cep == null) {
            Toast.makeText(this, "CEP não disponível para gerar relatório.", Toast.LENGTH_LONG).show()
            setRiskLevel("Indisponível", android.R.color.darker_gray)
            return
        }

        Log.d("RelatorioSeguranca", "Buscando relatório para o CEP: $cep")
        tvNivelRisco.text = "Avaliando..."

        apiService.getRelatorioSeguranca(cep)
            .enqueue(object : Callback<ApiService.RelatorioSegurancaResponse> {
                override fun onResponse(
                    call: Call<ApiService.RelatorioSegurancaResponse>,
                    response: Response<ApiService.RelatorioSegurancaResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val report = response.body()!!
                        updateReportUI(report)
                    } else {
                        onFailure(call, Exception("Resposta não sucedida: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<ApiService.RelatorioSegurancaResponse>, t: Throwable) {
                    Log.e("RelatorioSeguranca", "Falha na API: ${t.message}")
                    Toast.makeText(
                        this@RelatorioSegurancaActivity,
                        "Erro de conexão ao buscar relatório.",
                        Toast.LENGTH_SHORT
                    ).show()
                    setRiskLevel("Erro de Rede", android.R.color.darker_gray)
                    tvFurtoRoubos.text = "N/A"
                    tvVandalismo.text = "N/A"
                    tvAssedio.text = "N/A"
                    tvAtividadeSuspeita.text = "N/A"
                }
            })
    }

    private fun updateReportUI(report: ApiService.RelatorioSegurancaResponse) {
        // 1. Atualiza as estatísticas
        tvFurtoRoubos.text = report.furtoRouboCount.toString()
        tvVandalismo.text = report.vandalismoCount.toString()
        tvAssedio.text = report.assedioCount.toString()
        tvAtividadeSuspeita.text = report.atividadeSuspeitaCount.toString()

        // 2. Calcula e define o Nível de Risco (Este bloco está seguro)
        val total = report.totalOcorrencias
        val riskText = when {
            total > RISCO_ALTO_THRESHOLD -> "RISCO ALTO"
            total > RISCO_ALTO_THRESHOLD / 2 -> "RISCO MODERADO"
            else -> "RISCO BAIXO"
        }
        val colorResId = when {
            total > RISCO_ALTO_THRESHOLD -> R.color.black // Usando a cor black temporariamente
            total > RISCO_ALTO_THRESHOLD / 2 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_green_dark
        }
        setRiskLevel(riskText, colorResId)


        // 3. Atualiza as ocorrências recentes usando o layout estilizado
        llOcorrenciasRecentes.removeAllViews()

        if (report.ocorrenciasRecentes.isNotEmpty()) {
            llOcorrenciasRecentes.visibility = View.VISIBLE

            val inflater = LayoutInflater.from(this)

            report.ocorrenciasRecentes.forEach { item ->

                val ocorrenciaView = inflater.inflate(R.layout.item_ocorrencia_recente, llOcorrenciasRecentes, false)

                val tvDescricao = ocorrenciaView.findViewById<TextView>(R.id.tv_ocorrencia_descricao)
                val tvFonte = ocorrenciaView.findViewById<TextView>(R.id.tv_ocorrencia_fonte)
                val tvData = ocorrenciaView.findViewById<TextView>(R.id.tv_data)

                // Dados de ocorrência
                val tipoOcorrencia = item.tipo_ocorrencia ?: "Ocorrência Desconhecida"

                // LÓGICA DE DESCRIÇÃO: Usa a descrição real. Só usa o fallback se for null.
                val descricaoFonte = item.descricao ?: "Detalhes não fornecidos"

                val dataHora = item.data_hora ?: "" // String bruta da API (espera-se YYYY-MM-DD)

                // LÓGICA DE FORMATAÇÃO DE DATA BRASILEIRA POR EXTENSO
                var dataFormatada = "N/A"

                val datePart = if (dataHora.length >= 10) dataHora.substring(0, 10) else null

                if (datePart != null) {
                    if (datePart.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {

                        // Extrai Dia e Mês
                        val day = datePart.substring(8, 10) // DD
                        val monthIndex = datePart.substring(5, 7).toIntOrNull()?.minus(1) ?: -1 // 0-11

                        // Formata para DD\nNome do Mês (Ex: 30\nNOV)
                        dataFormatada = if (monthIndex in nomesMeses.indices) {
                            "$day\n${nomesMeses[monthIndex]}"
                        } else {
                            // Se o mês for inválido, mantém a formatação DD/MM
                            "$day\n${datePart.substring(5, 7)}"
                        }
                    }
                }

                // Preenche os dados
                tvDescricao.text = tipoOcorrencia

                // CORREÇÃO: Remove a data da tvFonte, deixando apenas a descrição
                tvFonte.text = descricaoFonte

                // Define a data formatada no formato DD\nNome do Mês
                tvData.text = dataFormatada

                // Adiciona a View estilizada (o CardView) ao container
                llOcorrenciasRecentes.addView(ocorrenciaView)
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

    private fun setRiskLevel(text: String, colorResId: Int) {
        tvNivelRisco.text = text

        try {
            // Acessa o LinearLayout diretamente pelo ID, que é a forma segura.
            val color = ContextCompat.getColor(this, colorResId)
            llNivelRiscoContainer.setBackgroundColor(color)

        } catch (e: Exception) {
            Log.e("RelatorioSeguranca", "Cor não encontrada ou erro ao aplicar. Usando vermelho padrão. Erro: ${e.message}")
            val color = ContextCompat.getColor(this, android.R.color.holo_red_dark)
            llNivelRiscoContainer.setBackgroundColor(color)
        }
    }
}