package com.example.negociomx_hyundai.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.negociomx_hyundai.BE.ActividadDiariaItem
import com.example.negociomx_hyundai.R

class ActividadDiariaAdapter(
    private var actividades: List<ActividadDiariaItem>,
    private val onItemClick: (ActividadDiariaItem) -> Unit
) : RecyclerView.Adapter<ActividadDiariaAdapter.ActividadViewHolder>() {

    class ActividadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvVIN: TextView = itemView.findViewById(R.id.tvVIN)
        val tvVehiculo: TextView = itemView.findViewById(R.id.tvVehiculo)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvFechaHora: TextView = itemView.findViewById(R.id.tvFechaHora)
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuario)
        val tvDetalles: TextView = itemView.findViewById(R.id.tvDetalles)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActividadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_consulta_actividad_diaria, parent, false)
        return ActividadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActividadViewHolder, position: Int) {
        val actividad = actividades[position]

        holder.tvVIN.text = "VIN: ${actividad.VIN}"
        holder.tvVehiculo.text = "${actividad.Marca} ${actividad.Modelo} ${actividad.Anio}"
        holder.tvStatus.text = actividad.NombreStatus

        // Formatear fecha y hora
        val fechaHora = try {
            val partes = actividad.FechaMovimiento.split(" ")
            if (partes.size >= 2) {
                val fecha = partes[0]
                val hora = partes[1].substring(0, 5) // Solo HH:mm
                "$fecha $hora"
            } else {
                actividad.FechaMovimiento
            }
        } catch (e: Exception) {
            actividad.FechaMovimiento
        }
        holder.tvFechaHora.text = fechaHora

        holder.tvUsuario.text = "Usuario: ${actividad.UsuarioMovimiento}"

        // Mostrar detalles según el tipo de actividad
        val detalles = when (actividad.IdStatus) {
            170 -> { // POSICIONADO
                if (!actividad.Bloque.isNullOrEmpty()) {
                    "Bloque: ${actividad.Bloque}, Pos: ${actividad.Fila}-${actividad.Columna}"
                } else {
                    "Sin ubicación específica"
                }
            }
            168, 169 -> { // ENTRADA, SALIDA
                if (!actividad.PlacaTransporte.isNullOrEmpty()) {
                    "Transporte: ${actividad.PlacaTransporte}"
                } else {
                    "Sin transporte registrado"
                }
            }
            else -> {
                if (!actividad.PersonaQueHaraMovimiento.isNullOrEmpty()) {
                    "Personal: ${actividad.PersonaQueHaraMovimiento}"
                } else {
                    "BL: ${actividad.BL}"
                }
            }
        }
        holder.tvDetalles.text = detalles

        // Color del status
        val colorStatus = when (actividad.IdStatus) {
            168 -> "#4CAF50" // ENTRADA - Verde
            169 -> "#FF5722" // SALIDA - Rojo
            170 -> "#FF9800" // POSICIONADO - Naranja
            171 -> "#2196F3" // EN TALLER - Azul
            else -> "#757575" // Otros - Gris
        }

        try {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor(colorStatus))
        } catch (e: Exception) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#757575"))
        }

        holder.itemView.setOnClickListener {
            onItemClick(actividad)
        }
    }

    override fun getItemCount(): Int = actividades.size

    fun actualizarActividades(nuevasActividades: List<ActividadDiariaItem>) {
        this.actividades = nuevasActividades
        notifyDataSetChanged()
    }
}