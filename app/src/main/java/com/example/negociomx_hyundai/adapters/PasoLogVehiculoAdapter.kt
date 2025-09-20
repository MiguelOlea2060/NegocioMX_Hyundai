package com.example.negociomx_hyundai.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.negociomx_hyundai.BE.PasoLogVehiculo
import com.example.negociomx_hyundai.R

class PasoLogVehiculoAdapter(
    private var registros: List<PasoLogVehiculo>,
    private val onItemClick: (PasoLogVehiculo) -> Unit
) : RecyclerView.Adapter<PasoLogVehiculoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVIN: TextView = view.findViewById(R.id.tvVIN)
        val tvBL: TextView = view.findViewById(R.id.tvBL)
        val tvMarcaModelo: TextView = view.findViewById(R.id.tvMarcaModelo)
        val tvAnio: TextView = view.findViewById(R.id.tvAnio)
        val tvColores: TextView = view.findViewById(R.id.tvColores)
        val tvNumeroMotor: TextView = view.findViewById(R.id.tvNumeroMotor)
        val tvDatosPasoLogVehiculoDet: TextView = view.findViewById(R.id.tvDatosPasoLogVehiculoDet)
        val tvTotalStatus: TextView = view.findViewById(R.id.tvFotos)
        val tvFechaHora: TextView = view.findViewById(R.id.tvFechaHora)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_consulta_paso1_soc, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val registro = registros[position]

        var fila:String=""
        var columna:String=""
        if(registro.Detalles!=null && registro.IdStatusActual==170)
        {
            var det=registro.Detalles!!.filter { it.IdStatus==170 }.firstOrNull()
            if(det!=null)
            {
                fila=det.Fila.toString()
                columna=det.Columna.toString()
            }
        }

        holder.tvVIN.text = "VIN: ${registro.Vin}"
        holder.tvBL.text = "BL: ${registro.BL}"
        holder.tvMarcaModelo.text = "${registro.Marca} ${registro.Modelo}"
        holder.tvAnio.text = "Año: ${registro.Anio}"

//        val modoTransporte = if (registro.ModoTransporte) "Sí" else "No"
//        val requiereRecarga = if (registro.RequiereRecarga) "Sí" else "No"
        holder.tvDatosPasoLogVehiculoDet.text = "Status actual: ${registro.NombreStatus} | Posicion -> Columna: ${columna}, Fila: ${fila} | "

        holder.tvTotalStatus.text = "📸 ${registro.CantidadStatus} statu(s)"
        holder.tvFechaHora.text = registro.FechaAlta

        holder.itemView.setOnClickListener {
            onItemClick(registro)
        }
    }

    override fun getItemCount() = registros.size

    fun actualizarRegistros(nuevosRegistros: List<PasoLogVehiculo>) {
        registros = nuevosRegistros
        notifyDataSetChanged()
    }
}
