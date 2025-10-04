package com.example.negociomx_hyundai.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.negociomx_hyundai.BE.MovimientoTracking
import com.example.negociomx_hyundai.R

class TrackingAdapter(
    private var movimientos: List<MovimientoTracking>
) : RecyclerView.Adapter<TrackingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFechaTracking)
        val tvHora: TextView = view.findViewById(R.id.tvHoraTracking)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusTracking)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalleTracking)
        val tvUsuario: TextView = view.findViewById(R.id.tvUsuarioTracking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tracking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movimiento = movimientos[position]

        holder.tvFecha.text = movimiento.FechaMovimiento
        holder.tvHora.text = movimiento.HoraMovimiento
        holder.tvStatus.text = movimiento.NombreStatus
        holder.tvDetalle.text = if (movimiento.Detalle.isNotEmpty()) movimiento.Detalle else "Sin detalles"
        holder.tvUsuario.text = "Usuario: ${movimiento.Usuario}"
    }

    override fun getItemCount() = movimientos.size

    fun actualizarMovimientos(nuevosMovimientos: List<MovimientoTracking>) {
        movimientos = nuevosMovimientos
        notifyDataSetChanged()
    }
}