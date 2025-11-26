import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Esta classe define o JSON enviado e recebido.
// O uso de @SerializedName é crucial para garantir a compatibilidade com o PHP.
@Parcelize
data class Ocorrencia(
    @SerializedName("id_ocorrencia")
    val id_ocorrencia: Int?,

    @SerializedName("id_usuario")
    val id_usuario: Int, // Certifique-se de que o PHP espera "id_usuario"

    @SerializedName("tipo_ocorrencia")
    val tipo_ocorrencia: String,

    @SerializedName("descricao")
    val descricao: String,

    @SerializedName("data_hora")
    val data_hora: String,

    @SerializedName("latitude")
    val latitude: String, // Mantendo como String para precisão

    @SerializedName("longitude")
    val longitude: String, // Mantendo como String para precisão

    @SerializedName("cep") // ATENÇÃO AQUI: Assumindo que o PHP espera MINÚSCULAS
    val CEP: String,

    @SerializedName("endereco")
    val endereco: String,

    @SerializedName("validada")
    val validada: Int = 0, // Definido como 0 por padrão para a criação

    @SerializedName("caminho_arquivo")
    val caminho_arquivo: String? = null // Pode ser nulo
) : Parcelable