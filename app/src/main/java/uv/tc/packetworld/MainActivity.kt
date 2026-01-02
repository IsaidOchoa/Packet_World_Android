package uv.tc.packetworld

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityMainBinding
import uv.tc.packetworld.dto.RSAutenticacionColaborador
import uv.tc.packetworld.poko.Colaborador
import uv.tc.packetworld.util.Constantes
import android.util.Base64
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var colaborador: Colaborador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar datos iniciales
        cargarDatosIniciales()

        // Configurar eventos
        configurarEventos()
    }

    private fun cargarDatosIniciales() {
        try {
            val jsonColaborador: String? = intent.getStringExtra("colaborador")
            if (!jsonColaborador.isNullOrEmpty()) {
                val gson = Gson()
                val respuestaLogin = gson.fromJson(jsonColaborador, RSAutenticacionColaborador::class.java)
                colaborador = respuestaLogin.colaborador!!

                // Actualizar la interfaz completa
                actualizarInterfaz()
            } else {
                Toast.makeText(this, "No se recibió información del colaborador", Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar la información del colaborador", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun configurarEventos() {
        binding.ivEditarConductor.setOnClickListener {
            val gson = Gson()
            val jsonColaborador = gson.toJson(colaborador)
            val intent = Intent(this, EdicionConductorActivity::class.java).apply {
                putExtra("colaborador", jsonColaborador)
            }
            editarConductor.launch(intent) // ✅ Usa el launcher correcto
        }

        binding.ivSeleccionFoto.setOnClickListener {
            // ✅ Arreglo para Android: usar selector de galería
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            seleccionarFotoPerfil.launch(intent)
        }

        binding.btnEnvios.setOnClickListener {
            val gson = Gson()
            val jsonColaborador = gson.toJson(colaborador)
            val intent = Intent(this, ListaEnviosActivity::class.java).apply {
                putExtra("colaborador", jsonColaborador)
            }
            startActivity(intent)
        }

        binding.btnUnidad.setOnClickListener {
            Toast.makeText(this, "Funcionalidad de Unidad aún no implementada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarInterfaz() {
        // ✅ Actualiza TODOS los elementos visuales
        binding.tvNumeroPersonal.text = colaborador.numeroPersonal
        binding.tvNombreCompleto.text =
            "${colaborador.nombre} ${colaborador.apellidoPaterno} ${colaborador.apellidoMaterno}"
        binding.tvSucursal.text = "Sucursal: ${colaborador.idSucursal}"
        binding.tvRol.text = "Rol: ${colaborador.rol}"

        // ✅ Mostrar foto de perfil (de la BD o por defecto)
        mostrarFotoPerfil()
    }

    private fun mostrarFotoPerfil() {
        if (!colaborador.fotoBase64.isNullOrEmpty()) {
            try {
                val imgBytes = Base64.decode(colaborador.fotoBase64, Base64.DEFAULT)
                // ✅ Verificar que los bytes no estén vacíos
                if (imgBytes.isNotEmpty()) {
                    val imgBitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                    if (imgBitmap != null) {
                        binding.ivFotoPerfil.setImageBitmap(imgBitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Opcional: mostrar imagen por defecto si falla
            }
        }
        // Si no hay foto, el ImageView mostrará su imagen por defecto (o quedará vacío)
    }

    // =============== ACTUALIZACIÓN DE FOTO ===============
    private fun editarFotoPerfil(fotoBase64: String) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        val gson = Gson()
        val fotoObj = mapOf(
            "idColaborador" to colaborador.idColaborador,
            "fotoBase64" to fotoBase64
        )
        val json = gson.toJson(fotoObj)

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}colaborador/editar-foto")
            .setJsonObjectBody(gson.fromJson(json, com.google.gson.JsonObject::class.java))
            .asString()
            .setCallback { e, result ->
                runOnUiThread {
                    if (e == null && !result.isNullOrBlank()) {
                        try {
                            val respuesta = gson.fromJson(result, uv.tc.packetworld.dto.Respuesta::class.java)
                            if (!respuesta.error) {
                                // ✅ Actualizar foto local y refrescar UI
                                colaborador = colaborador.copy(fotoBase64 = fotoBase64)
                                mostrarFotoPerfil()
                                Toast.makeText(this, "Foto actualizada correctamente", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Error: ${respuesta.mensaje}", Toast.LENGTH_LONG).show()
                            }
                        } catch (ex: Exception) {
                            Toast.makeText(this, "Error al procesar respuesta", Toast.LENGTH_LONG).show()
                            ex.printStackTrace()
                        }
                    } else {
                        Toast.makeText(this, "Error de conexión", Toast.LENGTH_LONG).show()
                        e?.printStackTrace()
                    }
                }
            }
    }

    // =============== MANEJO DE RESULTADOS ===============
    private val editarConductor = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val jsonActualizado = result.data?.getStringExtra("colaborador_actualizado")
            if (!jsonActualizado.isNullOrEmpty()) {
                val gson = Gson()
                try {
                    val colaboradorActualizado = gson.fromJson(jsonActualizado, Colaborador::class.java)
                    colaborador = colaboradorActualizado
                    // ✅ FORZAR REFRESCO COMPLETO DE LA UI
                    actualizarInterfaz()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val seleccionarFotoPerfil = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val fotoBase64 = uriToBase64(uri)
                if (fotoBase64 != null) {
                    editarFotoPerfil(fotoBase64)
                } else {
                    Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Selección de imagen cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                // ✅ Usar opciones para evitar OutOfMemory
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inSampleSize = 2 // Reduce resolución
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                val baos = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val bytes = baos.toByteArray()
                baos.close()
                Base64.encodeToString(bytes, Base64.DEFAULT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}