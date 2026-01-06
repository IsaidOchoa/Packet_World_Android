// package uv.tc.packetworld
package uv.tc.packetworld

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityUnidadBinding
import uv.tc.packetworld.poko.Unidad
import uv.tc.packetworld.util.Constantes

class UnidadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnidadBinding
    private var idColaborador = 0

    private val tipoUnidadMap = mapOf(
        1 to "Gasolina",
        2 to "Diesel",
        3 to "Eléctrica",
        4 to "Híbrida"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnidadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }

        idColaborador = intent.getIntExtra("idColaborador", 0)
        if (idColaborador <= 0) {
            finishWithError("Acceso no autorizado")
            return
        }

        cargarUnidadDelConductor(idColaborador)
    }

    private fun cargarUnidadDelConductor(idColaborador: Int) {
        val url = "${Constantes().URL_API}unidad/buscar-por-colaborador/$idColaborador"

        Ion.with(this)
            .load(url)
            .asJsonObject()
            .setCallback { e, json ->
                runOnUiThread {
                    if (e != null) {
                        e.printStackTrace()
                        finishWithError("Error de conexión")
                        return@runOnUiThread
                    }

                    if (json == null) {
                        mostrarMensajeSinUnidad()
                        return@runOnUiThread
                    }

                    try {
                        val unidad = Gson().fromJson(json.toString(), Unidad::class.java)
                        mostrarDatosUnidad(unidad)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        finishWithError("Error al procesar los datos de la unidad")
                    }
                }
            }
    }

    private fun mostrarDatosUnidad(unidad: Unidad) {

        binding.tvMarca.text = unidad.marca ?: "N/A"
        binding.tvModelo.text = unidad.modelo ?: "N/A"
        binding.tvAnio.text = unidad.anio?.toString() ?: "N/A"
        binding.tvVin.text = unidad.vin ?: "N/A"
        binding.tvNii.text = unidad.nii ?: "N/A"

        val tipoTexto = tipoUnidadMap[unidad.idTipoUnidad] ?: "Desconocido (${unidad.idTipoUnidad})"
        binding.tvTipoUnidad.text = tipoTexto

        binding.llDatosUnidad.visibility = android.view.View.VISIBLE
        binding.cvMensajeSinUnidad.visibility = android.view.View.GONE
    }

    private fun mostrarMensajeSinUnidad() {
        binding.llDatosUnidad.visibility = android.view.View.GONE
        binding.cvMensajeSinUnidad.visibility = android.view.View.VISIBLE
    }

    private fun finishWithError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}