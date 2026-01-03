package uv.tc.packetworld.poko

data class Colaborador(
    val idColaborador: Int = 0,
    val nombre: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "", // ← Cambiado a no nullable
    val curp: String = "",
    val correo: String = "",          // ← Cambiado a no nullable
    val numeroPersonal: String = "",
    val numeroLicencia: String = "",  // ← Cambiado a no nullable
    val idRol: Int = 0,
    val rol: String = "",
    val idSucursal: Int = 0,
    val nombreSucursal: String = "",
    val fotoBase64: String? = null
)