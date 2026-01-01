package uv.tc.packetworld.dto

import uv.tc.packetworld.poko.Conductor

data class RSAutenticacionConductor(
    val error : Boolean,
    val mensaje : String,
    var conductor: Conductor?
)
