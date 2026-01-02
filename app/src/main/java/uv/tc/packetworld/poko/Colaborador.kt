package uv.tc.packetworld.poko

data class Colaborador(
    var nombre: String,
    var apellidoPaterno: String,
    var apellidoMaterno: String,
    var correo: String?,
    val numeroPersonal: String,
    val rol: String,
    val idRol: Int,
    val idSucursal: Int,
    val curp: String?,
    var fotoBase64: String? = null
)