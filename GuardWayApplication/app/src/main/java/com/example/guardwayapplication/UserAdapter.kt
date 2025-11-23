// src/main/java/com/example/guardwayapplication/UserAdapter.kt
package com.example.guardwayapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Interface to handle click events on items
interface OnUserActionsListener {
    fun onUserDelete(userId: Int, position: Int)
    // You can add other actions here, like editing
    // fun onUserEdit(userId: Int)
}

class UserAdapter(
    private val dataSet: MutableList<Usuario>,
    private val listener: OnUserActionsListener
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nome: TextView = view.findViewById(R.id.nomeUsuario)
        val email: TextView = view.findViewById(R.id.emailUsuario)
        // Corrected the button IDs. Please ensure these IDs match your XML layout.
        val btnExcluir: ImageButton = view.findViewById(R.id.btnExcluir)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false) // Ensure you have an 'item_usuario.xml' layout file
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val usuario = dataSet[position]
        viewHolder.nome.text = usuario.USUARIO_NOME
        viewHolder.email.text = usuario.USUARIO_EMAIL

        viewHolder.btnExcluir.setOnClickListener {
            // Assuming 'USUARIO_ID' exists in your Usuario class to be consistent with other fields.
            listener.onUserDelete(usuario.USUARIO_ID, position)
        }

        // Example for edit button
        // viewHolder.btnEditar.setOnClickListener {
        //     listener.onUserEdit(usuario.USUARIO_ID)
        // }
    }

    override fun getItemCount() = dataSet.size

    fun removeUser(position: Int) {
        if (position >= 0 && position < dataSet.size) {
            dataSet.removeAt(position)
            notifyItemRemoved(position) // Notifica o RecyclerView da remoção
        }
    }
}
