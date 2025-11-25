package com.example.guardwayapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ocorrencia(
    val id_ocorrencia: Int? = null,
    val id_usuario: Int,
    val tipo_ocorrencia: String,
    val descricao: String,
    val data_hora: String,
    val latitude: String,
    val longitude: String,
    val CEP: String,
    val endereco: String? = null,
    val validada: Int? = null,
    val caminho_arquivo: String? = null
) : Parcelable