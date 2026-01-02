package uv.tc.packetworld.poko
data class Envio(
    val numeroGuia: String,
    val direccionDestino: String,
    var estatus: String,

    // Para el detalle
    val sucursalOrigen: String? = null,
    val nombreDestinatario: String? = null,
    val direccionCompleta: String? = null,
    val paquetes: List<Paquete>? = null,

    // Contacto del cliente
    val nombreCliente: String? = null,
    val telefonoCliente: String? = null,
    val correoCliente: String? = null
)
