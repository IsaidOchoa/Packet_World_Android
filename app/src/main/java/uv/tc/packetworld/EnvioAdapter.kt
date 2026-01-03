package uv.tc.packetworld

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uv.tc.packetworld.databinding.ItemEnvioBinding
import uv.tc.packetworld.poko.Envio

class EnvioAdapter(
    private val envios: MutableList<Envio> = mutableListOf(),
    private val onEnvioClick: (Envio) -> Unit
) : RecyclerView.Adapter<EnvioAdapter.EnvioViewHolder>() {

    fun updateEnvios(newEnvios: List<Envio>) {
        envios.clear()
        envios.addAll(newEnvios)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnvioViewHolder {
        val binding = ItemEnvioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EnvioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EnvioViewHolder, position: Int) {
        val envio = envios[position]
        holder.bind(envio)
        holder.itemView.setOnClickListener {
            onEnvioClick(envio)
        }
    }

    override fun getItemCount() = envios.size

    class EnvioViewHolder(private val binding: ItemEnvioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(envio: Envio) {
            binding.tvGuia.text = envio.numeroGuia

            // ✅ Construir dirección completa a partir de los campos disponibles
            val direccionCompleta = "${envio.calleDestino} ${envio.numeroDestino}".trim()
            binding.tvDireccion.text = if (direccionCompleta.isNotEmpty()) direccionCompleta else "Sin dirección"

            binding.tvEstatus.text = envio.estatus ?: "Sin estatus"
        }
    }
}