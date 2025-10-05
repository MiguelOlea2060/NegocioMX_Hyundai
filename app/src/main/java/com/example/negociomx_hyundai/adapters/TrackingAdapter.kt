package com.example.negociomx_hyundai.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.negociomx_hyundai.BE.MovimientoTracking
import com.example.negociomx_hyundai.R
import com.example.negociomx_hyundai.Utils.Utils

class TrackingAdapter(
    private var movimientos: List<MovimientoTracking>
) : RecyclerView.Adapter<TrackingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFechaHoraTracking)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusTracking)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalleTracking)
    }

    val util=Utils()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tracking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movimiento = movimientos[position]

        var fechaHora:String=""
        if(movimiento.fecha.toString().isNotEmpty()) {
            fechaHora=util.formatearFecha(movimiento.fecha)
        }

        var detalles:String=""
        if (movimiento.detalle.isNotEmpty()) {
            detalles= movimiento.detalle
        }

        holder.tvFecha.text = fechaHora
        holder.tvStatus.text = movimiento.status
        holder.tvDetalle.text = detalles
    }

    override fun getItemCount() = movimientos.size

    fun actualizarMovimientos(nuevosMovimientos: List<MovimientoTracking>) {
        movimientos = nuevosMovimientos
        notifyDataSetChanged()
    }
}