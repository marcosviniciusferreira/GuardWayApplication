import com.example.guardwayapplication.LoginResponse
import com.example.guardwayapplication.Usuario
import com.example.guardwayapplication.UsuarioMainActivity
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/apis/login.php")
    fun login(
        @Query("usuario") usuario: String,
        @Query("senha") senha: String
    ): Call<List<LoginResponse>>

    //-------------------- CRUD PARA USUARIOS --------------------
    @GET("/apis/get_usuarios.php")
    fun getUsuarios(): Call<List<Usuario>>

    @DELETE("/apis/delete_usuario.php")
    fun deleteUsuario(@Query("id") userId: Int): Call<Void>

    // ATUALIZAÇÃO CORRIGIDA: Usa @POST para evitar o erro 405 no servidor
    @POST("/apis/update_usuario.php")
    fun updateUsuario(@Body usuario: Usuario): Call<SuccessResponse>

    // Método para criar um novo usuário (POST)
    @POST("/apis/create_usuario.php")
    fun createUsuario(@Body usuario: Usuario): Call<SuccessResponse>

    //-------------------- CRUD PARA OCORRENCIAS --------------------

    @GET("/apis/get_ocorrencias.php")
    fun getOcorrencias(): Call<List<Ocorrencia>>

    @GET("apis/get_ocorrencias_cep.php")
    fun getOcorrenciasPorCep(@Query("cep") cep: String): Call<List<OcorrenciaItem>>

    @DELETE("/apis/delete_ocorrencia.php")
    fun deleteOcorrencia(@Query("id") userId: Int): Call<Void>

    @PUT("/apis/update_ocorrencia.php")
    fun updateOcorrencia(@Body Ocorrencia: Ocorrencia): Call<SuccessResponse>

    @POST("/apis/create_ocorrencia.php")
    fun createOcorrencia(@Body Ocorrencia: Ocorrencia): Call<SuccessResponse>

    @GET("relatorio_seguranca/{cep}")
    fun getRelatorioSeguranca(@Query("cep") cep: String): Call<RelatorioSegurancaResponse>
    data class SuccessResponse(
        val success: Boolean,
        val message: String?
    )

    // NO SEU ARQUIVO ApiService.kt
    data class OcorrenciaGroupedItem(
        val tipo_ocorrencia: String, // Ex: "Roubos de carro"
        val count: Int
    )

    data class RelatorioSegurancaResponse(
        // Estatísticas resumidas
        val roubosCarroCount: Int,
        val roubosCelularCount: Int,
        val assaltosCount: Int,
        val atividadeSuspeitaCount: Int,
        val totalOcorrencias: Int, // Total geral

        // Lista para a seção "Ocorrências Recentes" (Exemplo, você pode usar o mesmo OcorrenciaItem da outra Activity)
        val ocorrenciasRecentes: List<OcorrenciaItem> // Assumindo que OcorrenciaItem já está definido
    )

    data class OcorrenciaCepResponse(
        val status: String,
        val count: Int,
        val address: String? = null
    )

    data class OcorrenciaItem(
        val id_ocorrencia: Int,
        val id_usuario: Int,
        val tipo_ocorrencia: String,
        val descricao: String,
        val data_hora: String,
        val latitude: Double,
        val longitude: Double,
        val cep: String,
        val validada: Int,
        val caminho_arquivo: String,
        val endereco: String?
    )
}