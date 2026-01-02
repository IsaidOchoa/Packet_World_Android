package uv.tc.packetworld

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityDetalleEnvioBinding
import uv.tc.packetworld.poko.Envio
import uv.tc.packetworld.util.Constantes

class DetalleEnvioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleEnvioBinding
    private lateinit var envio: Envio
    private lateinit var numeroPersonal: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleEnvioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jsonEnvio = intent.getStringExtra("envio") ?: run {
            Toast.makeText(this, "Envío no recibido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        numeroPersonal = intent.getStringExtra("numeroPersonal") ?: ""

        envio = Gson().fromJson(jsonEnvio, Envio::class.java)

        mostrarDatosEnvio()
        configurarSpinnerYBoton()
    }

    private fun mostrarDatosEnvio() {
        binding.tvNumeroGuia.text = envio.numeroGuia
        binding.tvSucursalOrigen.text = envio.sucursalOrigen ?: "N/A"
        binding.tvDestinatario.text = envio.nombreDestinatario ?: "N/A"
        binding.tvDireccionCompleta.text = envio.direccionCompleta ?: envio.direccionDestino
        binding.tvEstatusActual.text = "Estatus actual: ${envio.estatus}"

        // Mostrar paquetes
        val paquetesText = envio.paquetes?.joinToString("\n") { p ->
            "📦 ${p.descripcion} (${p.peso} kg)"
        } ?: "Sin paquetes"
        binding.tvPaquetes.text = paquetesText

        // Contacto cliente
        binding.tvNombreCliente.text = envio.nombreCliente ?: "N/A"
        binding.tvTelefonoCliente.text = envio.telefonoCliente ?: "N/A"
        binding.tvCorreoCliente.text = envio.correoCliente ?: "N/A"
    }

    private fun configurarSpinnerYBoton() {
        val estatusOpciones = arrayOf("En tránsito", "Detenido", "Entregado", "Cancelado")
        binding.spinnerEstatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estatusOpciones)

        // Seleccionar el estatus actual
        val indexActual = estatusOpciones.indexOf(envio.estatus)
        if (indexActual >= 0) binding.spinnerEstatus.setSelection(indexActual)

        binding.btnActualizarEstatus.setOnClickListener {
            val nuevoEstatus = binding.spinnerEstatus.selectedItem.toString()
            val comentario = binding.etComentario.text.toString().trim()

            // Validación: comentario obligatorio si es "Detenido" o "Cancelado"
            if ((nuevoEstatus == "Detenido" || nuevoEstatus == "Cancelado") && comentario.isEmpty()) {
                Toast.makeText(this, "El comentario es obligatorio para este estatus.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            actualizarEstatusEnServidor(envio.numeroGuia, nuevoEstatus, comentario)
        }
    }

    private fun actualizarEstatusEnServidor(guia: String, estatus: String, comentario: String) {
        val url = "${Constantes().URL_API}envios/actualizar-estatus"
        val body = """
            {
                "numeroGuia": "$guia",
                "estatus": "$estatus",
                "comentario": "$comentario",
                "numeroPersonal": "$numeroPersonal"
            }
        """.trimIndent()

        Ion.with(this)
            .load("PUT", url)
            .setJsonObjectBody(Gson().fromJson(body, com.google.gson.JsonObject::class.java))
            .asJsonObject()
            .setCallback { e, result ->
                if (e == null) {
                    Toast.makeText(this, "Estatus actualizado correctamente", Toast.LENGTH_SHORT).show()
                    envio.estatus = estatus // Actualizar localmente
                    binding.tvEstatusActual.text = "Estatus actual: $estatus"
                } else {
                    Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}