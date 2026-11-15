package com.nendo.argosy.data.emulator

object Vita3kLinkMatcher {
    data class LinkGame(
        val gameId: Long,
        val title: String,
        val titleId: String?,
        val saveId: String?,
        val rommFileName: String?,
        val fileNames: List<String>
    )

    data class InstalledApp(val titleId: String, val title: String?)

    data class Match(val gameId: Long, val gameTitle: String, val titleId: String, val appTitle: String?)

    data class Result(val sure: List<Match>, val byTitle: List<Match>)

    private val TITLE_ID = Regex("""^[A-Z]{4}\d{5}$""")
    private val BRACKETED = Regex("""\[([A-Z]{4}\d{5})]""")
    private val CONTENT_ID = Regex("""^[A-Z]{2}\d{4}-([A-Z]{4}\d{5})_\d{2}-[A-Z0-9]{16}""")
    private val NOT_ALNUM = Regex("""[^\p{L}\p{N}]+""")

    /**
     * Pairs [games] with [installed] apps. A sure pair rests on a title id the game already
     * carries; a title pair only on matching names. Either kind is kept only when the game and
     * the app pick each other and nobody else; a title id in [held] is never offered.
     */
    fun match(games: List<LinkGame>, installed: List<InstalledApp>, held: Set<String>): Result {
        val apps = installed
            .filter { it.titleId.uppercase() !in held }
            .associateBy { it.titleId.uppercase() }
        val sure = uniquePairs(games, apps.values.toList()) { game, app -> app.titleId.uppercase() in idHints(game) }
        val pairedGames = sure.map { it.gameId }.toSet()
        val pairedApps = sure.map { it.titleId }.toSet()
        val leftGames = games.filter { it.gameId !in pairedGames && idHints(it).isEmpty() }
        val leftApps = apps.values.filter { it.titleId.uppercase() !in pairedApps && it.title != null }
        val byTitle = uniquePairs(leftGames, leftApps) { game, app -> titlesMatch(game.title, app.title!!) }
        return Result(sure, byTitle)
    }

    fun idHints(game: LinkGame): Set<String> = buildSet {
        listOfNotNull(game.titleId, game.saveId)
            .map { it.trim().uppercase() }
            .filter { TITLE_ID.matches(it) }
            .forEach(::add)
        game.rommFileName?.let { name -> BRACKETED.findAll(name).forEach { add(it.groupValues[1]) } }
        game.fileNames.forEach { name -> CONTENT_ID.find(name)?.let { add(it.groupValues[1]) } }
    }

    fun titlesMatch(gameTitle: String, appTitle: String): Boolean {
        val a = words(gameTitle)
        val b = words(appTitle)
        if (a.isEmpty() || b.isEmpty()) return false
        val (shorter, longer) = if (a.size <= b.size) a to b else b to a
        return longer.subList(0, shorter.size) == shorter
    }

    private fun words(title: String): List<String> =
        NOT_ALNUM.split(title.lowercase()).filter { it.isNotEmpty() }

    private fun uniquePairs(
        games: List<LinkGame>,
        apps: List<InstalledApp>,
        fits: (LinkGame, InstalledApp) -> Boolean
    ): List<Match> {
        val appsByGame = games.associateWith { game -> apps.filter { fits(game, it) } }
        return appsByGame.mapNotNull { (game, candidates) ->
            val app = candidates.singleOrNull() ?: return@mapNotNull null
            val rivals = games.count { other -> appsByGame[other].orEmpty().any { it.titleId == app.titleId } }
            if (rivals != 1) return@mapNotNull null
            Match(game.gameId, game.title, app.titleId.uppercase(), app.title)
        }
    }
}
