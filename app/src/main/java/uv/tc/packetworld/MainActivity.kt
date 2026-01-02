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
import uv.tc.packetworld.dto.RSAutenticacionColaborador
import uv.tc.packetworld.poko.Colaborador
import uv.tc.packetworld.util.Constantes
import java.io.ByteArrayOutputStream
import android.util.Base64

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var colaborador: Colaborador  // ✅ Cambiado de "conductor" a "colaborador"
    private var fotoPerfilBytes: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mostrarInformacionColaborador()
    }

    override fun onStart() {
        super.onStart()
        descargarFotoColaborador(colaborador.numeroPersonal)

        binding.ivEditarConductor.setOnClickListener {
            val gson = Gson()
            val jsonColaborador = gson.toJson(colaborador)  // ✅ Enviamos el objeto completo
            val intent = Intent(this, EdicionConductorActivity::class.java).apply {
                putExtra("colaborador", jsonColaborador)  // ✅ Cambiado a "colaborador"
            }
            startActivity(intent)
        }

        binding.ivSeleccionFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            seleccionarFotoPerfil.launch(intent)
        }
    }

    private fun mostrarInformacionColaborador() {
        try {
            val jsonColaborador: String? = intent.getStringExtra("colaborador")  // ✅ Cambiado a "colaborador"
            if (!jsonColaborador.isNullOrEmpty()) {
                val gson = Gson()
                val respuestaLogin: RSAutenticacionColaborador =
                    gson.fromJson(jsonColaborador, RSAutenticacionColaborador::class.java)
                colaborador = respuestaLogin.colaborador!!  // ✅ Cambiado a "colaborador"

                // Mostrar datos en UI
                binding.tvNumeroPersonal.text = colaborador.numeroPersonal
                binding.tvNombreCompleto.text =
                    "${colaborador.nombre} ${colaborador.apellidoPaterno} ${colaborador.apellidoMaterno}"

                // ✅ Ajuste: en tu JSON, el campo es "idSucursal", no "sucursal"
                binding.tvSucursal.text = "Sucursal: ${colaborador.idSucursal}"  // O usa nombreSucursal si lo tienes

                binding.tvRol.text = "Rol: ${colaborador.rol}"
            } else {
                Toast.makeText(this, "No se recibió información del colaborador", Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar la información del colaborador", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun descargarFotoColaborador(numeroPersonal: String) {
        Ion.with(this)
            .load("GET", "${Constantes().URL_API}conductor/obtener-foto/$numeroPersonal")
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
                val colaboradorApi: Colaborador = gson.fromJson(json, Colaborador::class.java)  // ✅ Cambiado a Colaborador
                if (!colaboradorApi.fotoBase64.isNullOrEmpty()) {
                    val imgBytes = Base64.decode(colaboradorApi.fotoBase64, Base64.DEFAULT)
                    val imgBitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                    binding.ivFotoPerfil.setImageBitmap(imgBitmap)
                } else {
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
            .load("PUT", "${Constantes().URL_API}conductor/subir-foto/${colaborador.numeroPersonal}")
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
                descargarFotoColaborador(colaborador.numeroPersonal)
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                baos.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}