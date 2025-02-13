package net.artemisitor.server.api.manager

object URLManager {
    const val PACK = "https://github.tbedu.top/https://github.com/Artemisia-Elre/PotatoCraft/raw/refs/heads/master/update/launcher/modpack/pack.zip"
    private const val GITHUB = "https://raw.gitmirror.com/Artemisia-Elre/PotatoCraft/refs/heads/master"
    fun getGithubUrl(value : String): String{
        return "$GITHUB$value"
    }
    private const val FORGER = "https://www.curseforge.com/api/v1/mods/{projectID}/files/{fileID}/download"
    fun getForgeUrl(project : String,file : String): String{
        return FORGER.replace("{projectID}", project).replace("{fileID}", file)
    }

}