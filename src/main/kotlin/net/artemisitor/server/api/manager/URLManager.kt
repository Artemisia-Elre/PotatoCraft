package net.artemisitor.server.api.manager

object URLManager {
    private const val GITEE = "https://gitee.com/aile123/PotatoCraft/raw/master/"
    fun getGiteeUrl(value : String): String{
        return GITEE + value
    }
    private const val FORGER = "https://www.curseforge.com/api/v1/mods/{projectID}/files/{fileID}/download"
    fun getForgeUrl(project : String,file : String): String{
        return FORGER.replace("{projectID}", project).replace("{fileID}", file)
    }

}