package com.nendo.argosy.ui.hela.vita

data class HelaVitaLinkRow(val gameId: Long, val gameTitle: String, val titleId: String, val appTitle: String?)

data class HelaVitaLinkState(
    val visible: Boolean = false,
    val rows: List<HelaVitaLinkRow> = emptyList(),
    val checked: Set<Long> = emptySet(),
    val focusIndex: Int = 0
) {
    val cancelIndex: Int get() = rows.size
    val confirmIndex: Int get() = rows.size + 1
}
