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
import android.util.Base64
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var colaborador: Colaborador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarDatosIniciales()
        configurarEventos()
    }

    private fun cargarDatosIniciales() {
        val jsonColaborador: String? = intent.getStringExtra("colaborador")
        if (jsonColaborador.isNullOrEmpty()) {
            Toast.makeText(this, "No se recibió información del colaborador", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            val gson = Gson()
            val respuestaLogin = gson.fromJson(jsonColaborador, RSAutenticacionColaborador::class.java)
            val colaboradorLogin = respuestaLogin.colaborador
            if (colaboradorLogin?.idColaborador == null || colaboradorLogin.idColaborador <= 0) {
                Toast.makeText(this, "Colaborador inválido", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            colaborador = colaboradorLogin
            cargarPerfilCompleto(colaborador.idColaborador)

        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar datos de inicio", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun cargarPerfilCompleto(id: Int) {
        val url = "${Constantes().URL_API}colaborador/perfil/$id"

        Ion.with(this)
            .load(url)
            .asJsonObject()
            .setCallback { e, json ->
                runOnUiThread {
                    if (e != null || json == null) {
                        Toast.makeText(this, "Error al cargar el perfil (con foto)", Toast.LENGTH_LONG).show()
                        e?.printStackTrace()
                        return@runOnUiThread
                    }

                    try {
                        val gson = Gson()
                        colaborador = gson.fromJson(json.toString(), Colaborador::class.java)
                        actualizarInterfaz()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        Toast.makeText(this, "Error al procesar el perfil", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun redirigirALogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun confirmarLogout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                redirigirALogin()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun configurarEventos() {
        binding.ivEditarConductor.setOnClickListener {
            val gson = Gson()
            val colaboradorLigero = colaborador.copy(fotoBase64 = null)
            val jsonLigero = gson.toJson(colaboradorLigero)
            val intent = Intent(this, EdicionConductorActivity::class.java).apply {
                putExtra("colaborador", jsonLigero)
            }
            editarConductor.launch(intent)
        }

        binding.ivSeleccionFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            seleccionarFotoPerfil.launch(intent)
        }

        binding.btnEnvios.setOnClickListener {
            val gson = Gson()
            val colaboradorLigero = colaborador.copy(fotoBase64 = null)
            val jsonLigero = gson.toJson(colaboradorLigero)
            val intent = Intent(this, ListaEnviosActivity::class.java).apply {
                putExtra("colaborador", jsonLigero)
            }
            startActivity(intent)
        }

        binding.btnUnidad.setOnClickListener {
            val intent = Intent(this, UnidadActivity::class.java).apply {
                putExtra("idColaborador", colaborador.idColaborador)
            }
            startActivity(intent)
        }

        binding.ivLogout.setOnClickListener {
            confirmarLogout()
        }
    }

    private fun actualizarInterfaz() {
        binding.tvNumeroPersonal.text = colaborador.numeroPersonal
        binding.tvNombreCompleto.text =
            "${colaborador.nombre} ${colaborador.apellidoPaterno} ${colaborador.apellidoMaterno}"
        binding.tvSucursal.text = "${colaborador.nombreSucursal}"
        binding.tvRol.text = "${colaborador.rol}"

        mostrarFotoPerfil()
    }

    private fun mostrarFotoPerfil() {
        if (!colaborador.fotoBase64.isNullOrEmpty()) {
            try {
                val imgBytes = Base64.decode(colaborador.fotoBase64, Base64.DEFAULT)
                if (imgBytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                    if (bitmap != null) {
                        binding.ivFotoPerfil.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun editarFotoPerfil(fotoBase64: String) {
        val gson = Gson()
        val cuerpo = mapOf(
            "idColaborador" to colaborador.idColaborador,
            "fotoBase64" to fotoBase64
        )

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}colaborador/editar-foto")
            .setJsonObjectBody(gson.toJsonTree(cuerpo).asJsonObject)
            .asString()
            .setCallback { e, result ->
                runOnUiThread {
                    if (e == null && !result.isNullOrBlank()) {
                        println("Respuesta del servidor al guardar foto: $result")

                        try {
                            val respuesta = gson.fromJson(result, uv.tc.packetworld.dto.Respuesta::class.java)
                            if (!respuesta.error) {
                                colaborador = colaborador.copy(fotoBase64 = fotoBase64)
                                mostrarFotoPerfil()
                                Toast.makeText(this, "Foto actualizada correctamente", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Error del servidor: ${respuesta.mensaje}", Toast.LENGTH_LONG).show()
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            Toast.makeText(this, "Error al procesar respuesta: ${ex.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "Error de conexión al guardar foto", Toast.LENGTH_LONG).show()
                        e?.printStackTrace()
                    }
                }
            }
    }

    // MANEJO DE RESULTADOS
    private val editarConductor = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val jsonActualizado = result.data?.getStringExtra("colaborador_actualizado")
            if (!jsonActualizado.isNullOrEmpty()) {
                try {
                    val gson = Gson()
                    val colaboradorActualizado = gson.fromJson(jsonActualizado, Colaborador::class.java)
                    colaborador = colaboradorActualizado
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
            uri?.let { safeUri ->
                val fotoBase64 = uriToBase64(safeUri)
                if (fotoBase64 != null) {
                    editarFotoPerfil(fotoBase64)
                } else {
                    Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inSampleSize = 2
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                val baos = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val bytes = baos.toByteArray()
                baos.close()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}