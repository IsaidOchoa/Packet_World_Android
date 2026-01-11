package uv.tc.packetworld

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityLoginBinding
import uv.tc.packetworld.dto.RSAutenticacionColaborador
import uv.tc.packetworld.util.Constantes

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIngresar.setOnClickListener {
            verificarCredenciales()
        }
    }

    private fun verificarCredenciales() {
        if (sonCamposValidos()) {
            consumirAPI(
                numeroPersonal = binding.etNumeroPersonal.text.toString(),
                password = binding.etPassword.text.toString()
            )
        }
    }

    private fun sonCamposValidos(): Boolean {
        var valido = true

        val numeroPersonal = binding.etNumeroPersonal.text.toString().trim()

        if (numeroPersonal.isEmpty()) {
            binding.etNumeroPersonal.error = "Número Personal obligatorio"
            valido = false
        } else if (numeroPersonal.length > 30) {
            binding.etNumeroPersonal.error = "Máximo 30 caracteres"
            valido = false
        } else if (!numeroPersonal.matches(Regex("^[A-Za-z0-9\\-]+\$"))) {
            binding.etNumeroPersonal.error = "Solo letras, números y guion (-)"
            valido = false
        } else {
            binding.etNumeroPersonal.error = null
        }

        if (binding.etPassword.text.isNullOrBlank()) {
            binding.etPassword.error = "Contraseña obligatoria"
            valido = false
        } else {
            binding.etPassword.error = null
        }

        return valido
    }

    private fun consumirAPI(numeroPersonal: String, password: String) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load("POST", "${Constantes().URL_API}colaborador/login-movil")
            .setHeader("Content-Type", "application/x-www-form-urlencoded")
            .setBodyParameter("noPersonal", numeroPersonal)
            .setBodyParameter("password", password)
            .asString()
            .setCallback { e, result ->
                if (e == null) {
                    Log.e("LoginResponse", result)
                    serializarRespuesta(result)
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Error de conexión: ${e.message ?: "Verifique su conexión"}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("LoginError", e.toString())
                    }
                }
            }
    }

    private fun serializarRespuesta(json: String) {
        try {
            val gson = Gson()
            val respuestaLogin = gson.fromJson(json, RSAutenticacionColaborador::class.java)

            runOnUiThread {
                if (!respuestaLogin.error && respuestaLogin.colaborador != null) {
                    val colaborador = respuestaLogin.colaborador

                    // Validación de rol: solo conductores
                    if (colaborador.rol.equals("Conductor", ignoreCase = true)) {
                        Toast.makeText(
                            this,
                            "Bienvenido(a) ${colaborador.nombre}",
                            Toast.LENGTH_LONG
                        ).show()
                        irPantallaPrincipal(json)
                    } else {
                        Toast.makeText(
                            this,
                            "Acceso denegado: esta aplicación es solo para conductores",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    // Mensaje de error del backend
                    Toast.makeText(
                        this,
                        respuestaLogin.mensaje ?: "Credenciales incorrectas",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("LoginParseError", e.toString())
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Error al procesar la respuesta del servidor",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun irPantallaPrincipal(json: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("colaborador", json)
        startActivity(intent)
        finish()
    }
}