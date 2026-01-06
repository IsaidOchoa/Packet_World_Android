package uv.tc.packetworld.dto

import uv.tc.packetworld.poko.Colaborador
data class RSAutenticacionColaborador(
    val colaborador: Colaborador?,
    val error: Boolean,
    val mensaje: String?
)