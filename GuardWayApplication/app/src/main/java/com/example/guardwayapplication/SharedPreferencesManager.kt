package com.example.guardwayapplication

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {

    // Inicializa SharedPreferences e o Editor
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPrefs.edit()

    companion object {
        private const val PREF_NAME = "GuardWayPrefs"
        // Novas chaves
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_CPF = "user_cpf"
    }

    fun saveUserData(
        id: Int,
        nome: String,
        email: String,
        cpf: String
    ) {
        editor.putInt(KEY_USER_ID, id)
        editor.putString(KEY_USER_NAME, nome)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_CPF, cpf)
        editor.apply()
    }

    fun getUserId(): Int {
        return sharedPrefs.getInt(KEY_USER_ID, -1)
    }

    fun getUserName(): String? {
        return sharedPrefs.getString(KEY_USER_NAME, "")
    }

    fun getUserEmail(): String? {
        return sharedPrefs.getString(KEY_USER_EMAIL, "")
    }

    fun getUserCpf(): String? {
        return sharedPrefs.getString(KEY_USER_CPF, "")
    }

    /**
     * Limpa todos os dados salvos (logout).
     */
    fun clearData() {
        editor.clear()
        editor.apply()
    }
}