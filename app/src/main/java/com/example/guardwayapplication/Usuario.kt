package com.example.guardwayapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Usuario(
    val USUARIO_ID: Int,
    val USUARIO_NOME: String,
    val USUARIO_EMAIL: String,
    val USUARIO_CPF: String,
    val USUARIO_SENHA: String,
) : Parcelable