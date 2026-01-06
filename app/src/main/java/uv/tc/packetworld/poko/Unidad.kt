// package uv.tc.packetworld.poko
package uv.tc.packetworld.poko

data class Unidad(
    val idUnidad: Int?, // int(11) AI PK
    val marca: String?, // varchar(50)
    val modelo: String?, // varchar(50)
    val anio: Int?, // int(11)
    val vin: String?, // varchar(50)
    val nii: String?, // varchar(20)
    val idTipoUnidad: Int?, // int(11)
    val idSucursal: Int?, // int(11)
    val idColaborador: Int? // int(11)
)