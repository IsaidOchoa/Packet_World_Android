package uv.tc.packetworld.poko

data class Colaborador(
    val idColaborador: Int = 0,
    val nombre: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val curp: String = "",
    val correo: String = "",
    val numeroPersonal: String = "",
    val numeroLicencia: String = "",
    val idRol: Int = 0,
    val rol: String = "",
    val idSucursal: Int = 0,
    val nombreSucursal: String = "",
    val fotoBase64: String? = null
)