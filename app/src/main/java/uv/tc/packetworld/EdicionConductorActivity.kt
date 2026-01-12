package uv.tc.packetworld

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityEdicionConductorBinding
import uv.tc.packetworld.dto.Respuesta
import uv.tc.packetworld.poko.Password
import uv.tc.packetworld.poko.Colaborador
import uv.tc.packetworld.util.Constantes

class EdicionConductorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEdicionConductorBinding
    private lateinit var colaborador: Colaborador

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

        cargarDatosColaborador()

        // --- FLUJO ACTUALIZADO ---
        binding.btnActualizar.setOnClickListener {
            if (sonCamposValidos()) { // Validación local (formato)
                validarDuplicadosEnServidor { puedeContinuar -> // Validación remota (duplicados)
                    if (puedeContinuar) {
                        guardarCambios()
                    }
                }
            }
        }
        // -------------------------

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun cargarDatosColaborador() {
        val jsonColaborador = intent.getStringExtra("colaborador")
        if (jsonColaborador != null) {
            val gson = Gson()
            colaborador = gson.fromJson(jsonColaborador, Colaborador::class.java)

            binding.tvNumeroPersonal.text = colaborador.numeroPersonal
            binding.tvSucursal.text = "${colaborador.nombreSucursal}"
            binding.tvRol.text = colaborador.rol

            binding.etNombre.setText(colaborador.nombre)
            binding.etApellidoPaterno.setText(colaborador.apellidoPaterno)
            binding.etApellidoMaterno.setText(colaborador.apellidoMaterno)
            binding.etCorreo.setText(colaborador.correo ?: "")
            binding.etCurp.setText(colaborador.curp ?: "")

            if (colaborador.idRol == 3) {
                binding.etLicencia.visibility = View.VISIBLE
                binding.tvLabelLicencia.visibility = View.VISIBLE
                binding.etLicencia.setText(colaborador.numeroLicencia ?: "")
            } else {
                binding.etLicencia.visibility = View.GONE
                binding.tvLabelLicencia.visibility = View.GONE
            }
        } else {
            Toast.makeText(this, "No se recibieron los datos del colaborador", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // --- VALIDACIÓN LOCAL: Formato y campos obligatorios ---
    private fun sonCamposValidos(): Boolean {
        var valido = true

        // Validar nombre
        val nombre = binding.etNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            binding.etNombre.error = "Nombre obligatorio"
            valido = false
        } else if (nombre.length > 30) {
            binding.etNombre.error = "Máximo 30 caracteres"
            valido = false
        } else if (!nombre.matches(Regex("^[a-zA-ZáéíóúñÁÉÍÓÚÑ\\s]+\$"))) {
            binding.etNombre.error = "Solo letras y espacios"
            valido = false
        } else {
            binding.etNombre.error = null
        }

        // Validar apellido paterno
        val apellidoP = binding.etApellidoPaterno.text.toString().trim()
        if (apellidoP.isEmpty()) {
            binding.etApellidoPaterno.error = "Apellido paterno obligatorio"
            valido = false
        } else if (apellidoP.length > 30) {
            binding.etApellidoPaterno.error = "Máximo 30 caracteres"
            valido = false
        } else if (!apellidoP.matches(Regex("^[a-zA-ZáéíóúñÁÉÍÓÚÑ\\s]+\$"))) {
            binding.etApellidoPaterno.error = "Solo letras y espacios"
            valido = false
        } else {
            binding.etApellidoPaterno.error = null
        }

        // Validar apellido materno
        val apellidoM = binding.etApellidoMaterno.text.toString().trim()
        if (apellidoM.isNotEmpty()) {
            if (apellidoM.length > 30) {
                binding.etApellidoMaterno.error = "Máximo 30 caracteres"
                valido = false
            } else if (!apellidoM.matches(Regex("^[a-zA-ZáéíóúñÁÉÍÓÚÑ\\s]+\$"))) {
                binding.etApellidoMaterno.error = "Solo letras y espacios"
                valido = false
            } else {
                binding.etApellidoMaterno.error = null
            }
        }

        // Validar correo
        val correo = binding.etCorreo.text.toString().trim()
        if (correo.isEmpty()) {
            binding.etCorreo.error = "Correo obligatorio"
            valido = false
        } else if (correo.length > 40) {
            binding.etCorreo.error = "Máximo 40 caracteres"
            valido = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreo.error = "Correo inválido"
            valido = false
        } else {
            binding.etCorreo.error = null
        }

        // Validar CURP
        val curp = binding.etCurp.text.toString().trim()
        if (curp.isEmpty()) {
            binding.etCurp.error = "CURP obligatoria"
            valido = false
        } else if (curp.length != 18) {
            binding.etCurp.error = "La CURP debe tener exactamente 18 caracteres"
            valido = false
        } else if (!curp.matches(Regex("^[A-Z0-9]{18}\$"))) {
            binding.etCurp.error = "Solo letras mayúsculas y números"
            valido = false
        } else {
            binding.etCurp.error = null
        }

        // Validar licencia (si aplica)
        if (colaborador.idRol == 3) {
            val licencia = binding.etLicencia.text.toString().trim()
            if (licencia.isEmpty()) {
                binding.etLicencia.error = "Número de licencia obligatorio"
                valido = false
            } else if (licencia.length > 30) {
                binding.etLicencia.error = "Máximo 30 caracteres"
                valido = false
            } else if (!licencia.matches(Regex("^[A-Za-z0-9\\-]+\$"))) {
                binding.etLicencia.error = "Solo letras, números y guion (-)"
                valido = false
            } else {
                binding.etLicencia.error = null
            }
        }

        // Validación de contraseña
        if (!binding.etPasswordNueva.text.isNullOrBlank()) {
            if (binding.etPasswordActual.text.isNullOrBlank()) {
                binding.etPasswordActual.error = "Requerida para cambiar contraseña"
                valido = false
            } else {
                binding.etPasswordActual.error = null
            }

            if (binding.etPasswordNueva.text.toString() != binding.etPasswordConfirmar.text.toString()) {
                binding.etPasswordConfirmar.error = "Las contraseñas no coinciden"
                valido = false
            } else {
                binding.etPasswordConfirmar.error = null
            }

            if (binding.etPasswordNueva.text?.length ?: 0 < 8) {
                binding.etPasswordNueva.error = "Mínimo 8 caracteres"
                valido = false
            } else if (binding.etPasswordNueva.text.toString().contains(" ")) {
                binding.etPasswordNueva.error = "No se permiten espacios"
                valido = false
            } else {
                binding.etPasswordNueva.error = null
            }
        }

        return valido
    }
    // ----------------------------------------------------

    // --- VALIDACIÓN REMOTA: Duplicados en la BD ---
    private fun validarDuplicadosEnServidor(callback: (Boolean) -> Unit) {
        // Solo validar CURP y Licencia si han cambiado
        val curpParaValidar = if (binding.etCurp.text.toString() != colaborador.curp) {
            binding.etCurp.text.toString().trim()
        } else {
            "" // No validar si no ha cambiado
        }

        val licenciaParaValidar = if (colaborador.idRol == 3 &&
            binding.etLicencia.text.toString() != (colaborador.numeroLicencia ?: "")) {
            binding.etLicencia.text.toString().trim()
        } else {
            ""
        }

        // Si ninguno ha cambiado, continuar sin validación
        if (curpParaValidar.isEmpty() && licenciaParaValidar.isEmpty()) {
            callback(true)
            return
        }

        // Crear el objeto de validación
        val datosValidacion = mapOf(
            "curp" to curpParaValidar,
            "numeroLicencia" to licenciaParaValidar,
            "idColaboradorExcluir" to colaborador.idColaborador
        )

        Ion.with(this)
            .load("POST", "${Constantes().URL_API}colaborador/validar-duplicados")
            .setJsonObjectBody(Gson().toJsonTree(datosValidacion).asJsonObject)
            .asString()
            .setCallback { e, result ->
                runOnUiThread {
                    if (e == null && !result.isNullOrBlank()) {
                        try {
                            val respuesta = Gson().fromJson(result, Respuesta::class.java)
                            if (respuesta.error) {
                                Toast.makeText(this, respuesta.mensaje, Toast.LENGTH_LONG).show()
                                callback(false)
                            } else {
                                callback(true)
                            }
                        } catch (ex: Exception) {
                            Toast.makeText(this, "Error al procesar la validación", Toast.LENGTH_LONG).show()
                            callback(false)
                        }
                    } else {
                        Toast.makeText(this, "Error de conexión al validar duplicados", Toast.LENGTH_LONG).show()
                        callback(false)
                    }
                }
            }
    }
    // ------------------------------------------------

    private fun guardarCambios() {
        val operaciones = mutableListOf<String>()

        if (hayCambiosPerfil()) operaciones.add("perfil")
        if (!binding.etPasswordNueva.text.isNullOrBlank()) {
            operaciones.add("password")
        }
        if (operaciones.isEmpty()) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        var completadas = 0
        var errorOcurrido = false

        if (operaciones.contains("perfil")) {
            val perfilActualizado = Colaborador(
                nombre = binding.etNombre.text.toString(),
                apellidoPaterno = binding.etApellidoPaterno.text.toString(),
                apellidoMaterno = binding.etApellidoMaterno.text.toString(),
                correo = binding.etCorreo.text.toString().ifEmpty { "" },
                numeroLicencia = if (colaborador.idRol == 3) binding.etLicencia.text.toString().ifEmpty { "" } else colaborador.numeroLicencia,
                curp = binding.etCurp.text.toString(),
                numeroPersonal = colaborador.numeroPersonal,
                rol = colaborador.rol,
                idRol = colaborador.idRol,
                idSucursal = colaborador.idSucursal,
                fotoBase64 = colaborador.fotoBase64,
                idColaborador = colaborador.idColaborador
            )

            editarPerfil(perfilActualizado) { respuesta ->
                runOnUiThread {
                    if (respuesta?.error == true) {
                        Toast.makeText(this, "Error Perfil: ${respuesta.mensaje}", Toast.LENGTH_LONG).show()
                        errorOcurrido = true
                    } else {
                        completadas++
                    }
                    verificarFinalizacion(completadas, operaciones.size, errorOcurrido)
                }
            }
        }

        if (operaciones.contains("password")) {
            val passwordRequest = Password(
                idColaborador = colaborador.idColaborador,
                passwordActual = binding.etPasswordActual.text.toString(),
                passwordNueva = binding.etPasswordNueva.text.toString()
            )

            editarPassword(passwordRequest) { respuesta ->
                runOnUiThread {
                    if (respuesta?.error == true) {
                        Toast.makeText(this, "Error Contraseña: ${respuesta.mensaje}", Toast.LENGTH_LONG).show()
                        errorOcurrido = true
                    } else {
                        completadas++
                    }
                    verificarFinalizacion(completadas, operaciones.size, errorOcurrido)
                }
            }
        }
    }

    private fun editarPerfil(perfil: Colaborador, callback: (Respuesta?) -> Unit) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)
        val gson = Gson()
        val json = gson.toJson(perfil)

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}colaborador/editar-perfil")
            .setJsonObjectBody(gson.fromJson(json, JsonObject::class.java))
            .asString()
            .setCallback { e, result ->
                runOnUiThread {
                    if (e == null && !result.isNullOrBlank()) {
                        try {
                            val respuesta = gson.fromJson(result, Respuesta::class.java)
                            callback(respuesta)
                        } catch (ex: Exception) {
                            callback(null)
                        }
                    } else {
                        callback(null)
                    }
                }
            }
    }

    private fun editarPassword(request: Password, callback: (Respuesta?) -> Unit) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)
        val gson = Gson()
        val json = gson.toJson(request)

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}colaborador/editar-password")
            .setJsonObjectBody(gson.fromJson(json, JsonObject::class.java))
            .asString()
            .setCallback { e, result ->
                runOnUiThread {
                    if (e == null && !result.isNullOrBlank()) {
                        try {
                            val respuesta = gson.fromJson(result, Respuesta::class.java)
                            callback(respuesta)
                        } catch (ex: Exception) {
                            callback(null)
                        }
                    } else {
                        callback(null)
                    }
                }
            }
    }

    private fun verificarFinalizacion(completadas: Int, total: Int, errorOcurrido: Boolean) {
        if (completadas >= total && !errorOcurrido) {
            colaborador = colaborador.copy(
                nombre = binding.etNombre.text.toString(),
                apellidoPaterno = binding.etApellidoPaterno.text.toString(),
                apellidoMaterno = binding.etApellidoMaterno.text.toString(),
                correo = binding.etCorreo.text.toString().ifEmpty { "" },
                curp = binding.etCurp.text.toString(),
                numeroLicencia = if (colaborador.idRol == 3) binding.etLicencia.text.toString() else colaborador.numeroLicencia
            )

            val gson = Gson()
            val jsonActualizado = gson.toJson(colaborador)

            val intent = Intent()
            intent.putExtra("colaborador_actualizado", jsonActualizado)
            setResult(RESULT_OK, intent)
            Toast.makeText(this, "¡Cambios guardados correctamente!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun hayCambiosPerfil(): Boolean {
        return (binding.etNombre.text.toString() != colaborador.nombre ||
                binding.etApellidoPaterno.text.toString() != colaborador.apellidoPaterno ||
                binding.etApellidoMaterno.text.toString() != (colaborador.apellidoMaterno ?: "") ||
                binding.etCorreo.text.toString() != (colaborador.correo ?: "") ||
                binding.etCurp.text.toString() != (colaborador.curp ?: "") ||
                (colaborador.idRol == 3 && binding.etLicencia.text.toString() != (colaborador.numeroLicencia ?: "")))
    }
}