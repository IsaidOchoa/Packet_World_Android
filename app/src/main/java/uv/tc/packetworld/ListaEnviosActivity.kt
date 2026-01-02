package uv.tc.packetworld

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import uv.tc.packetworld.databinding.ActivityListaEnviosBinding
import uv.tc.packetworld.poko.Envio
import uv.tc.packetworld.util.Constantes

class ListaEnviosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaEnviosBinding
    private lateinit var adapter: EnvioAdapter
    private lateinit var colaborador: uv.tc.packetworld.poko.Colaborador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaEnviosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recibir colaborador
        val json = intent.getStringExtra("colaborador") ?: run {
            Toast.makeText(this, "Error: colaborador no recibido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        colaborador = Gson().fromJson(json, uv.tc.packetworld.poko.Colaborador::class.java)

        adapter = EnvioAdapter { envio ->
            // Al tocar un envío, abrir detalle
            val intent = Intent(this, DetalleEnvioActivity::class.java).apply {
                putExtra("envio", Gson().toJson(envio))
                putExtra("numeroPersonal", colaborador.numeroPersonal)
            }
            startActivity(intent)
        }

        binding.rvEnvios.layoutManager = LinearLayoutManager(this)
        binding.rvEnvios.adapter = adapter

        cargarEnviosDelConductor(colaborador.numeroPersonal)
    }

    private fun cargarEnviosDelConductor(numeroPersonal: String) {
        Ion.with(this)
            .load("${Constantes().URL_API}envios/conductor/$numeroPersonal")
            .asString()  // ← Cambia de .asJsonArray() a .asString()
            .setCallback { e, result ->
                if (e == null) {
                    // 🔎 IMPRIME EL JSON PARA VER SU ESTRUCTURA REAL
                    println("JSON RECIBIDO: $result")

                    try {
                        val gson = Gson()
                        // Si es un arreglo directo:
                        val envios = gson.fromJson(result, Array<Envio>::class.java).toList()
                        // Si es un objeto contenedor, ver paso 2
                        adapter.updateEnvios(envios)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        Toast.makeText(this, "Error al parsear: ${ex.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}