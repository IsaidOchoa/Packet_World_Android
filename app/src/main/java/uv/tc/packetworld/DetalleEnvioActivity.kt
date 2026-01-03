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
    private lateinit var numeroPersonal: String
    private lateinit var numeroGuia: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleEnvioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            finish() // Esto regresará a ListaEnviosActivity
        }

        // Recibir datos mínimos
        numeroGuia = intent.getStringExtra("numeroGuia") ?: run {
            finishWithError("Número de guía no recibido")
            return
        }
        numeroPersonal = intent.getStringExtra("numeroPersonal") ?: ""

        // Cargar el envío completo desde el backend
        cargarEnvioCompleto(numeroGuia)
        configurarSpinnerYBoton()
    }

    private fun cargarEnvioCompleto(guia: String) {
        Ion.with(this)
            .load("${Constantes().URL_API}envio/buscar/$guia")
            .asJsonObject()
            .setCallback { e, json ->
                if (e == null && json != null) {
                    try {
                        val envio = Gson().fromJson(json.toString(), Envio::class.java)
                        mostrarDatosEnvio(envio)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        finishWithError("Error al cargar detalles del envío")
                    }
                } else {
                    finishWithError("No se pudo cargar el envío")
                }
            }
    }

    private fun mostrarDatosEnvio(envio: Envio) {
        binding.tvNumeroGuia.text = envio.numeroGuia
        binding.tvSucursalOrigen.text = envio.nombreSucursalOrigen ?: "N/A"
        binding.tvDestinatario.text = envio.nombreDestinatario ?: "N/A"
        binding.tvDireccionCompleta.text = "${envio.calleDestino} ${envio.numeroDestino}"
        binding.tvEstatusActual.text = "Estatus actual: ${envio.estatus}"

        // Mostrar paquetes
        val paquetesText = envio.paquetes?.joinToString("\n") { p ->
            "📦 ${p.descripcion} (${p.peso} kg)"
        } ?: "Sin paquetes"
        binding.tvPaquetes.text = paquetesText

        // Datos del cliente (ajusta según lo que devuelva tu backend)
        binding.tvNombreCliente.text = envio.nombreCliente ?: "N/A"
        // Nota: Si tu backend no devuelve teléfono/correo, comenta estas líneas
        // binding.tvTelefonoCliente.text = envio.telefonoCliente ?: "N/A"
        // binding.tvCorreoCliente.text = envio.correoCliente ?: "N/A"
    }

    private fun configurarSpinnerYBoton() {
        val estatusOpciones = arrayOf("En tránsito", "Detenido", "Entregado", "Cancelado")
        binding.spinnerEstatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            estatusOpciones
        )

        // Configurar botón
        binding.btnActualizarEstatus.setOnClickListener {
            val nuevoEstatus = binding.spinnerEstatus.selectedItem.toString()
            val comentario = binding.etComentario.text.toString().trim()

            if ((nuevoEstatus == "Detenido" || nuevoEstatus == "Cancelado") && comentario.isEmpty()) {
                Toast.makeText(this, "El comentario es obligatorio para este estatus.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            actualizarEstatusEnServidor(numeroGuia, nuevoEstatus, comentario)
        }
    }

    private fun actualizarEstatusEnServidor(guia: String, estatus: String, comentario: String) {
        // Nota: Asegúrate de que este endpoint exista en tu backend
        val url = "${Constantes().URL_API}envio/actualizar-estatus"
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
                    // Actualizar UI
                    binding.tvEstatusActual.text = "Estatus actual: $estatus"
                } else {
                    Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun finishWithError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}