package net.artemisitor.server.api.json

class PackJson {
    data class Version(val string: String)
    
    data class ModLoader(
        val id: String,
        val primary: Boolean
    )

    data class File(
        val projectID: Int,
        val fileID: Int,
        val required: Boolean
    )

    data class Minecraft(
        val version: String,
        val modLoaders: List<ModLoader>
    )

    data class Manifest(
        val minecraft: Minecraft,
        val manifestType: String,
        val manifestVersion: Int,
        val name: String,
        val version: String,
        val author: String,
        val files: List<File>,
        val overrides : String
    )
}