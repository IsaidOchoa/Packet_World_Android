package uv.tc.packetworld

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityLoginBinding
import uv.tc.packetworld.dto.RSAutenticacionConductor
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
        if (binding.etNumeroPersonal.text.isEmpty()) {
            binding.etNumeroPersonal.error = "Número Personal obligatorio"
            valido = false
        }
        if (binding.etPassword.text.isEmpty()) {
            binding.etPassword.error = "Contraseña obligatoria"
            valido = false
        }
        return valido
    }

    private fun consumirAPI(numeroPersonal: String, password: String) {
        // Desactivar conscrypt si hay problemas de TLS (opcional, según tu backend)
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load("POST", "${Constantes.URL_API}autenticacion/conductor")
            .setHeader("Content-Type", "application/x-www-form-urlencoded")
            .setBodyParameter("numeroPersonal", numeroPersonal)
            .setBodyParameter("password", password)
            .asString()
            .setCallback { e, result ->
                if (e == null) {
                    Log.e("LoginResponse", result)
                    serializarRespuesta(result)
                } else {
                    Toast.makeText(this, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("LoginError", e.toString())
                }
            }
    }

    private fun serializarRespuesta(json: String) {
        try {
            val gson = Gson()
            val respuestaLogin = gson.fromJson(json, RSAutenticacionConductor::class.java)

            if (!respuestaLogin.error && respuestaLogin.conductor != null) {
                // 🔐 Verificación adicional: asegurarse de que el rol sea "Conductor"
                // Si usas ID de rol, podrías hacer: if (respuestaLogin.idRol == 3)
                // Pero asumiremos que el backend ya filtra, y solo validamos el texto
                if (respuestaLogin.conductor.rol.equals("Conductor", ignoreCase = true)) {
                    Toast.makeText(
                        this,
                        "Bienvenido(a) ${respuestaLogin.conductor.nombre}",
                        Toast.LENGTH_LONG
                    ).show()

                    irPantallaPrincipal(json)
                } else {
                    Toast.makeText(
                        this,
                        "Acceso denegado: solo conductores pueden iniciar sesión",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    respuestaLogin.mensaje ?: "Credenciales incorrectas",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e("LoginParseError", e.toString())
            Toast.makeText(
                this,
                "Error al procesar la respuesta del servidor",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun irPantallaPrincipal(json: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("conductor", json)
        startActivity(intent)
        finish()
    }
}