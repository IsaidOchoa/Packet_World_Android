// package uv.tc.packetworld.poko
package uv.tc.packetworld.poko

data class Envio(
    val idEnvio: Int = 0,
    val numeroGuia: String = "",
    val costo: Double = 0.0,
    val peso: Double = 0.0,
    val calleDestino: String = "",
    val numeroDestino: String = "",
    val nombreDestinatario: String = "",
    val idCliente: Int = 0,
    val idSucursalOrigen: Int = 0,
    val nombreSucursalOrigen: String = "",
    val idColoniaDestino: Int = 0,
    val nombreColonia: String = "",
    val idUnidad: Int = 0,
    val idConductor: Int = 0,
    val idEstadoActual: Int = 0,
    val estatus: String = "",

    // Campos adicionales del endpoint completo
    val nombreCliente: String? = null,
    val telefonoCliente: String? = null,
    val correoCliente: String? = null,
    val infoUnidad: String? = null,
    val nombreConductor: String? = null,
    val municipio: String? = null,
    val estado: String? = null,
    val codigoPostalDestino: String? = null,

    // Paquetes (solo en /buscar/{guia})
    val paquetes: List<Paquete>? = null
)