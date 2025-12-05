package com.example.guardwayapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

interface OnUserActionsListener {
    fun onUserDelete(userId: Int, position: Int)
    fun onUserEdit(usuario: Usuario)
    fun onUserAdd(usuario: Usuario)
}

class UserAdapter(
    private val dataSet: MutableList<Usuario>,
    private val listener: OnUserActionsListener
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nome: TextView = view.findViewById(R.id.nomeUsuario)
        val email: TextView = view.findViewById(R.id.emailUsuario)
        val btnExcluir: ImageButton = view.findViewById(R.id.btnExcluir)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Infla o layout CORRETO para um item da lista
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val usuario = dataSet[position]
        viewHolder.nome.text = usuario.USUARIO_NOME
        viewHolder.email.text = usuario.USUARIO_EMAIL

        // Listeners separados para cada botão
        viewHolder.btnExcluir.setOnClickListener {
            listener.onUserDelete(usuario.USUARIO_ID, position)
        }

        viewHolder.btnEditar.setOnClickListener {
            listener.onUserEdit(usuario)
        }
    }

    override fun getItemCount() = dataSet.size

    fun removeUser(position: Int) {
        if (position >= 0 && position < dataSet.size) {
            dataSet.removeAt(position)
            notifyItemRemoved(position)
            // Notifica o adapter que o range de itens mudou, para evitar crashes
            notifyItemRangeChanged(position, dataSet.size)
        }
    }
}