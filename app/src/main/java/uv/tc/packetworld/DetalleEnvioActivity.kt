package uv.tc.packetworld

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityDetalleEnvioBinding
import uv.tc.packetworld.dto.Respuesta
import uv.tc.packetworld.poko.Envio
import uv.tc.packetworld.util.Constantes

class DetalleEnvioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleEnvioBinding
    private var idEnvioActual = 0
    private var idColaborador = 0

    private val estatusAId = mapOf(
        "En tránsito" to 1,
        "Detenido" to 2,
        "Entregado" to 3,
        "Cancelado" to 4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleEnvioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }

        idColaborador = intent.getIntExtra("idColaborador", 0)
        if (idColaborador <= 0) {
            finishWithError("Acceso no autorizado")
            return
        }

        val numeroGuia = intent.getStringExtra("numeroGuia")
        if (numeroGuia.isNullOrEmpty()) {
            finishWithError("Número de guía no recibido")
            return
        }

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
                        finishWithError("Error al procesar datos del envío")
                    }
                } else {
                    finishWithError("No se pudo cargar el envío")
                }
            }
    }

    private fun mostrarDatosEnvio(envio: Envio) {
        idEnvioActual = envio.idEnvio ?: 0

        binding.tvNumeroGuia.text = envio.numeroGuia
        binding.tvSucursalOrigen.text = envio.nombreSucursalOrigen ?: "N/A"
        binding.tvDestinatario.text = envio.nombreDestinatario ?: "N/A"
        binding.tvDireccionCompleta.text =
            "${envio.calleDestino ?: ""} ${envio.numeroDestino ?: ""}"
        binding.tvEstatusActual.text = "Estatus actual: ${envio.estatus ?: "N/A"}"

        val paquetes = envio.paquetes?.joinToString("\n") {
            "📦 ${it.descripcion} (${it.peso} kg)"
        } ?: "Sin paquetes"

        binding.tvPaquetes.text = paquetes
        binding.tvNombreCliente.text = envio.nombreCliente ?: "N/A"
    }

    private fun configurarSpinnerYBoton() {
        val estatusOpciones = arrayOf("En tránsito", "Detenido", "Entregado", "Cancelado")
        binding.spinnerEstatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            estatusOpciones
        )

        binding.btnActualizarEstatus.setOnClickListener {
            val estatusTexto = binding.spinnerEstatus.selectedItem.toString()
            val comentario = binding.etComentario.text.toString().trim()

            if ((estatusTexto == "Detenido" || estatusTexto == "Cancelado") && comentario.isEmpty()) {
                Toast.makeText(
                    this,
                    "El comentario es obligatorio para este estatus",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val idEstado = estatusAId[estatusTexto]
            if (idEstado == null || idEnvioActual <= 0) {
                Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            actualizarEstatusEnServidor(idEnvioActual, idEstado, comentario, estatusTexto)
        }
    }

    private fun actualizarEstatusEnServidor(
        idEnvio: Int,
        idEstado: Int,
        comentario: String?,
        estatusTexto: String
    ) {
        val body = mutableMapOf<String, Any>(
            "idEnvio" to idEnvio,
            "idEstadoActual" to idEstado,
            "idColaborador" to idColaborador
        )

        if (!comentario.isNullOrEmpty()) {
            body["comentario"] = comentario
        }

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}envio/actualizar-estatus-movil")
            .setJsonObjectBody(Gson().toJsonTree(body).asJsonObject)
            .asJsonObject()
            .setCallback { e, result ->
                if (e != null || result == null) {
                    Toast.makeText(
                        this,
                        "Error al actualizar estatus",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setCallback
                }

                val respuesta = Gson().fromJson(result, Respuesta::class.java)
                if (!respuesta.error) {
                    Toast.makeText(this, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                    binding.tvEstatusActual.text = "Estatus actual: $estatusTexto"
                    binding.etComentario.text.clear()
                } else {
                    Toast.makeText(this, respuesta.mensaje, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun finishWithError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
