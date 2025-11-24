import com.example.guardwayapplication.LoginResponse
import com.example.guardwayapplication.Usuario
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

    // Data Class para resposta de sucesso (se aplicável ao seu projeto)
    data class SuccessResponse(
        val success: Boolean,
        val message: String?
    )
}