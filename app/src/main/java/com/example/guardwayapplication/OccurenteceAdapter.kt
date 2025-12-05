package com.example.guardwayapplication

import Ocorrencia
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class OccurrenceAdapter(
    private val ocorrencias: MutableList<Ocorrencia>,
    private val listener: OnOccurrenceActionsListener
) : RecyclerView.Adapter<OccurrenceAdapter.OccurrenceViewHolder>() {

    class OccurrenceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // IDs atualizadas para corresponder ao item_ocorrencia.xml (com botões e data estilizada)
        val textTipo: TextView = view.findViewById(R.id.tipoOcorrencia) // Título da Ocorrência
        val textEndereco: TextView = view.findViewById(R.id.enderrecoOcorrencia) // Endereço
        val textDia: TextView = view.findViewById(R.id.tv_dia) // Dia do Mês (Bloco Estilizado)
        val textMes: TextView = view.findViewById(R.id.tv_mes) // Mês (Bloco Estilizado)
        val textId: TextView = view.findViewById(R.id.textOccurrenceId) // ID: 101
        val textDataHora: TextView = view.findViewById(R.id.textOccurrenceDataHora) // Data/Hora completa (na linha de baixo)

        val btnEdit: ImageButton = view.findViewById(R.id.btnEditOccurrence)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteOccurrence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OccurrenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ocorrencia, parent, false)
        return OccurrenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: OccurrenceViewHolder, position: Int) {
        val ocorrencia = ocorrencias[position]

        // Exibindo os dados da ocorrência
        holder.textTipo.text = "\"${ocorrencia.tipo_ocorrencia}\"" // Adiciona aspas para seguir o protótipo
        holder.textEndereco.text = ocorrencia.endereco
        holder.textId.text = "Ocorrência #: ${ocorrencia.id_ocorrencia}"

        // Formata e exibe a data/hora
        val formattedDate = formatDateTime(ocorrencia.data_hora)
        holder.textDia.text = formattedDate.day
        holder.textMes.text = formattedDate.month
        holder.textDataHora.text = formattedDate.fullDateTime // Data/Hora na linha de baixo

        // Ações de clique
        holder.btnEdit.setOnClickListener {
            listener.onOccurrenceEdit(ocorrencia)
        }

        holder.btnDelete.setOnClickListener {
            // Chama a função de exclusão na Activity
            // Certifica-se de que id_ocorrencia não é nulo antes de chamar a exclusão
            ocorrencia.id_ocorrencia?.let { id ->
                listener.onOccurrenceDelete(id, position)
            }
        }
    }

    /**
     * Objeto auxiliar para retornar a data formatada
     */
    private data class FormattedDate(
        val day: String,
        val month: String,
        val fullDateTime: String
    )

    /**
     * Formata a string de data (Ex: "2023-10-27 20:30:00") para Day, Month e FullDateTime.
     */
    private fun formatDateTime(dateTime: String?): FormattedDate {
        dateTime ?: return FormattedDate("00", "DATA", "Data Indisponível")

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputDayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val outputMonthFormat = SimpleDateFormat("MMMM", Locale("pt", "BR")) // Para mostrar o mês em Português
        val outputFullDateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        return try {
            val date = inputFormat.parse(dateTime)

            // Capitaliza a primeira letra do mês
            val monthName = outputMonthFormat.format(date).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString()
            }

            FormattedDate(
                day = outputDayFormat.format(date),
                month = monthName,
                fullDateTime = outputFullDateTimeFormat.format(date)
            )
        } catch (e: Exception) {
            FormattedDate("00", "ERRO", "Formato de Data Inválido")
        }
    }

    override fun getItemCount(): Int = ocorrencias.size

    fun removeOccurrence(position: Int) {
        if (position >= 0 && position < ocorrencias.size) {
            ocorrencias.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}