package uv.tc.packetworld.dto

import uv.tc.packetworld.poko.Colaborador
data class RSAutenticacionColaborador(
    val colaborador: Colaborador?, // ← mismo nombre que en el JSON
    val error: Boolean,
    val mensaje: String?
)