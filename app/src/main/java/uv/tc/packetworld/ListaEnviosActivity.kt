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
    private lateinit var numeroPersonal: String // ✅ Solo esto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaEnviosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            finish() // Esto regresará a ListaEnviosActivity
        }

        //Leer el colaborador del Intent y extraer numeroPersonal
        val json = intent.getStringExtra("colaborador") ?: run {
            Toast.makeText(this, "Error: colaborador no recibido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val colaborador = Gson().fromJson(json, uv.tc.packetworld.poko.Colaborador::class.java)
        numeroPersonal = colaborador.numeroPersonal

        adapter = EnvioAdapter { envio ->
            val intent = Intent(this, DetalleEnvioActivity::class.java).apply {
                putExtra("numeroGuia", envio.numeroGuia)
                putExtra("numeroPersonal", numeroPersonal) // ✅ Usamos la variable local
            }
            startActivity(intent)
        }

        binding.rvEnvios.layoutManager = LinearLayoutManager(this)
        binding.rvEnvios.adapter = adapter

        cargarEnviosDelConductor(numeroPersonal) // ✅ Usamos la variable local
    }

    private fun cargarEnviosDelConductor(numeroPersonal: String) {
        Ion.with(this)
            .load("${Constantes().URL_API}envio/conductor/$numeroPersonal")
            .asJsonArray()
            .setCallback { e, jsonArray ->
                if (e == null && jsonArray != null) {
                    try {
                        val envios = Gson().fromJson(jsonArray, Array<Envio>::class.java).toList()
                        adapter.updateEnvios(envios)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        Toast.makeText(this, "Error al procesar envíos", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "No se pudieron cargar los envíos", Toast.LENGTH_LONG).show()
                    e?.printStackTrace()
                }
            }
    }
}