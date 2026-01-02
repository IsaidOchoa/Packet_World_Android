package uv.tc.packetworld

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityEdicionConductorBinding
import uv.tc.packetworld.dto.Respuesta
import uv.tc.packetworld.poko.Colaborador
import uv.tc.packetworld.util.Constantes

class EdicionConductorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEdicionConductorBinding
    private lateinit var colaborador: Colaborador  // ✅ Cambiado a Colaborador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEdicionConductorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarDatosColaborador()  // ✅ Cambiado

        binding.btnActualizar.setOnClickListener {
            if (sonCamposValidos()) {
                editarDatosColaborador()  // ✅ Cambiado
            }
        }
    }

    private fun cargarDatosColaborador() {  // ✅ Cambiado
        val jsonColaborador = intent.getStringExtra("colaborador")  // ✅ Cambiado a "colaborador"
        if (jsonColaborador != null) {
            val gson = Gson()
            colaborador = gson.fromJson(jsonColaborador, Colaborador::class.java)

            // Mostrar datos NO editables (solo lectura)
            binding.tvNumeroPersonal.text = colaborador.numeroPersonal
            // ✅ Ajuste: en tu JSON, el campo es "idSucursal", no "sucursal"
            binding.tvSucursal.text = "Sucursal: ${colaborador.idSucursal}"  // O usa nombreSucursal si lo tienes
            binding.tvRol.text = colaborador.rol  // Ya es el nombre del rol

            // Cargar datos editables en campos de texto
            binding.etNombre.setText(colaborador.nombre)
            binding.etApellidoPaterno.setText(colaborador.apellidoPaterno)
            binding.etApellidoMaterno.setText(colaborador.apellidoMaterno)
            binding.etCorreo.setText(colaborador.correo)
        } else {
            Toast.makeText(this, "No se recibieron los datos del colaborador", Toast.LENGTH_LONG).show()  // ✅ Cambiado
            finish()
        }
    }

    private fun sonCamposValidos(): Boolean {
        var valido = true

        if (binding.etNombre.text.isNullOrBlank()) {
            binding.etNombre.error = "Nombre obligatorio"
            valido = false
        }
        if (binding.etApellidoPaterno.text.isNullOrBlank()) {
            binding.etApellidoPaterno.error = "Apellido paterno obligatorio"
            valido = false
        }
        // Apellido materno opcional
        if (binding.etCorreo.text.isNullOrBlank()) {
            binding.etCorreo.error = "Correo obligatorio"
            valido = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.etCorreo.text).matches()) {
            binding.etCorreo.error = "Correo inválido"
            valido = false
        }

        return valido
    }

    private fun editarDatosColaborador() {  // ✅ Cambiado
        // Actualizar solo los campos editables
        colaborador.nombre = binding.etNombre.text.toString()
        colaborador.apellidoPaterno = binding.etApellidoPaterno.text.toString()
        colaborador.apellidoMaterno = binding.etApellidoMaterno.text.toString()
        colaborador.correo = binding.etCorreo.text.toString()

        val gson = Gson()
        val colaboradorJson = gson.toJson(colaborador)  // ✅ Cambiado

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}conductor/editar")
            .setHeader("Content-Type", "application/json")
            .setStringBody(colaboradorJson)  // ✅ Cambiado
            .asString()
            .setCallback { e, result ->
                runOnUiThread {
                    if (e == null) {
                        try {
                            val respuesta = gson.fromJson(result, Respuesta::class.java)
                            if (!respuesta.error) {
                                Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_LONG).show()
                                // Regresar a MainActivity con los nuevos datos
                                val intent = Intent()
                                intent.putExtra("colaborador_actualizado", colaboradorJson)  // ✅ Cambiado
                                setResult(RESULT_OK, intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Error: ${respuesta.mensaje}", Toast.LENGTH_LONG).show()
                            }
                        } catch (ex: Exception) {
                            Toast.makeText(this, "Error al procesar la respuesta", Toast.LENGTH_LONG).show()
                            ex.printStackTrace()
                        }
                    } else {
                        Toast.makeText(this, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }
            }
    }
}