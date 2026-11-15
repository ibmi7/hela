package com.nendo.argosy.data.emulator

object Vita3kInstalledApps {
    private val SAVE_DATA = Regex("""^(.*)/ux0/user/\d{2}/savedata$""")
    private const val DECRYPTED_SUFFIX = "_dec"

    fun appsDirOf(saveDataPath: String): String? =
        SAVE_DATA.find(saveDataPath.trimEnd('/'))?.groupValues?.get(1)?.let { "$it/ux0/app" }

    /**
     * Whether [titleId] has a folder in the `ux0/app` beside [saveDataPath], matched as Vita3K
     * matches `-r`: case-insensitively, folders ending in `_dec` left out. Null when the answer
     * is unknown: no save path, a path of another shape, or an app folder [list] cannot read.
     */
    fun isInstalled(saveDataPath: String?, titleId: String, list: (String) -> List<String>?): Boolean? {
        val appsDir = saveDataPath?.let(::appsDirOf) ?: return null
        val names = list(appsDir) ?: return null
        return names.any { !it.endsWith(DECRYPTED_SUFFIX) && it.equals(titleId, ignoreCase = true) }
    }
}
