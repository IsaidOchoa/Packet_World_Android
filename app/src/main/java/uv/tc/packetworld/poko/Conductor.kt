package uv.tc.packetworld.poko

data class Conductor(
    var numeroPersonal: String,
    var nombre: String,
    var apellidoPaterno: String,
    var apellidoMaterno: String,
    var sucursal: String,
    var rol: String,
    var correo: String?,
    var fotoBase64: String?
)