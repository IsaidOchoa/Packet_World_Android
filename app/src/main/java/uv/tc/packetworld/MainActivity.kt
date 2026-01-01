package uv.tc.packetworld

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityMainBinding
import uv.tc.packetworld.dto.RSAutenticacionConductor
import uv.tc.packetworld.poko.Conductor
import uv.tc.packetworld.util.Constantes
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.util.Base64


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conductor: Conductor
    private var fotoPerfilBytes: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mostrarInformacionConductor()
    }

    override fun onStart() {
        super.onStart()
        // Cargar foto desde API
        descargarFotoConductor(conductor.numeroPersonal)

        // Configurar clic en ícono de edición (si se permite)
        binding.ivEditarConductor.setOnClickListener {
            // Nota: según tus requisitos, algunos campos NO son editables (número personal, sucursal, rol)
            val gson = Gson()
            val jsonConductor = gson.toJson(conductor)
            val intent = Intent(this, EdicionConductorActivity::class.java).apply {
                putExtra("conductor", jsonConductor)
            }
            startActivity(intent)
        }

        // Permitir selección de foto (si el backend lo soporta)
        binding.ivSeleccionFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            seleccionarFotoPerfil.launch(intent)
        }
    }

    private fun mostrarInformacionConductor() {
        try {
            val jsonConductor: String? = intent.getStringExtra("conductor")
            if (!jsonConductor.isNullOrEmpty()) {
                val gson = Gson()
                val respuestaLogin: RSAutenticacionConductor =
                    gson.fromJson(jsonConductor, RSAutenticacionConductor::class.java)
                conductor = respuestaLogin.conductor!!
                // Mostrar datos en UI
                binding.tvNumeroPersonal.text = conductor.numeroPersonal
                binding.tvNombreCompleto.text =
                    "${conductor.nombre} ${conductor.apellidoPaterno} ${conductor.apellidoMaterno}"
                binding.tvSucursal.text = "Sucursal: ${conductor.sucursal}"
                binding.tvRol.text = "Rol: ${conductor.rol}"
            } else {
                Toast.makeText(this, "No se recibió información del conductor", Toast.LENGTH_LONG).show()
                finish() // o redirigir a login
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar la información del conductor", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun descargarFotoConductor(numeroPersonal: String) {
        Ion.with(this)
            .load("GET", "${Constantes.URL_API}conductor/obtener-foto/$numeroPersonal")
            .asString()
            .setCallback { e, result ->
                if (e == null) {
                    mostrarFotoPerfilAPI(result)
                } else {
                    Toast.makeText(this, "Error al descargar foto: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun mostrarFotoPerfilAPI(json: String) {
        try {
            if (json.isNotEmpty()) {
                val gson = Gson()
                val conductorApi: Conductor = gson.fromJson(json, Conductor::class.java)
                if (!conductorApi.fotoBase64.isNullOrEmpty()) {
                    val imgBytes = Base64.decode(conductorApi.fotoBase64, Base64.DEFAULT)
                    val imgBitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                    binding.ivFotoPerfil.setImageBitmap(imgBitmap)
                } else {
                    // Opcional: mostrar ícono predeterminado
                    Toast.makeText(this, "No tienes foto de perfil", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al mostrar la foto de perfil", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun subirFotoPerfil() {
        Ion.with(this)
            .load("PUT", "${Constantes.URL_API}conductor/subir-foto/${conductor.numeroPersonal}")
            .setByteArrayBody(fotoPerfilBytes)
            .asString()
            .setCallback { e, result ->
                if (e == null) {
                    verificarEnvioFoto(result)
                } else {
                    Toast.makeText(this, "Error al subir la foto: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun verificarEnvioFoto(result: String) {
        try {
            val gson = Gson()
            val respuesta = gson.fromJson(result, uv.tc.packetworld.dto.Respuesta::class.java)
            if (!respuesta.error) {
                Toast.makeText(this, respuesta.mensaje, Toast.LENGTH_LONG).show()
                descargarFotoConductor(conductor.numeroPersonal)
            } else {
                Toast.makeText(this, "Error: ${respuesta.mensaje}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al procesar respuesta", Toast.LENGTH_LONG).show()
        }
    }

    private val seleccionarFotoPerfil =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    fotoPerfilBytes = uriToByteArray(uri)
                    fotoPerfilBytes?.let {
                        subirFotoPerfil()
                    } ?: run {
                        Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Selección de imagen cancelada", Toast.LENGTH_SHORT).show()
            }
        }

    private fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos) // 90% calidad
                baos.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}