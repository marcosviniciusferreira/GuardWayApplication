// src/main/java/com/example/guardwayapplication/UserAdapter.kt
package com.example.guardwayapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// O adaptador recebe uma lista de objetos Usuario
class UserAdapter(private val dataSet: List<Usuario>) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Encontra os TextViews no seu item_usuario.xml
        val nome: TextView = view.findViewById(R.id.nomeUsuario)
        val email: TextView = view.findViewById(R.id.emailUsuario)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            // Infla o novo layout item_usuario
            .inflate(R.layout.item_usuario, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val usuario = dataSet[position]
        // Atribui os dados do objeto Usuario aos TextViews
        viewHolder.nome.text = usuario.USUARIO_NOME
        viewHolder.email.text = usuario.USUARIO_EMAIL
    }

    override fun getItemCount() = dataSet.size
}