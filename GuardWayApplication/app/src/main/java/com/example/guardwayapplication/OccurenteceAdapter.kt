package com.example.guardwayapplication

import Ocorrencia
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class OccurrenceAdapter(
    private val ocorrencias: MutableList<Ocorrencia>,
    private val listener: OnOccurrenceActionsListener
) : RecyclerView.Adapter<OccurrenceAdapter.OccurrenceViewHolder>() {

    class OccurrenceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // As IDs das Views aqui devem corresponder às IDs definidas em item_ocorrencia.xml
        val textId: TextView = view.findViewById(R.id.textOccurrenceId)
        val textTipo: TextView = view.findViewById(R.id.textOccurrenceTipo)
        val textEndereco: TextView = view.findViewById(R.id.textOccurrenceEndereco)
        val textDataHora: TextView = view.findViewById(R.id.textOccurrenceDataHora)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditOccurrence)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteOccurrence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OccurrenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            // Aqui está a referência para o layout do item
            .inflate(R.layout.item_ocorrencia, parent, false)
        return OccurrenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: OccurrenceViewHolder, position: Int) {
        val ocorrencia = ocorrencias[position]

        // Exibindo os dados da ocorrência
        holder.textId.text = "ID: ${ocorrencia.id_ocorrencia}"
        holder.textTipo.text = ocorrencia.tipo_ocorrencia
        holder.textEndereco.text = ocorrencia.endereco
        holder.textDataHora.text = formatDateTime(ocorrencia.data_hora)

        // Ações de clique
        holder.btnEdit.setOnClickListener {
            listener.onOccurrenceEdit(ocorrencia)
        }

        holder.btnDelete.setOnClickListener {
            // Chama a função de exclusão na Activity
            ocorrencia.id_ocorrencia?.let { id ->
                listener.onOccurrenceDelete(id, position)
            }
        }
    }

    // Função para formatar a data/hora (Exemplo simples)
    private fun formatDateTime(dateTime: String?): String {
        return dateTime?.substringBefore(' ') ?: "Data Indisponível"
    }

    override fun getItemCount(): Int = ocorrencias.size

    fun removeOccurrence(position: Int) {
        if (position >= 0 && position < ocorrencias.size) {
            ocorrencias.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}