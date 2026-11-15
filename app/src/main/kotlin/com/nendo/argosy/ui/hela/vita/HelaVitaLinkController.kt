package com.nendo.argosy.ui.hela.vita

import com.nendo.argosy.R
import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.showError
import com.nendo.argosy.core.notification.showSuccess
import com.nendo.argosy.data.emulator.Vita3kLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class HelaVitaLinkController @Inject constructor(
    private val vita3kLink: Vita3kLink,
    private val notificationManager: NotificationManager
) {
    private val _state = MutableStateFlow(HelaVitaLinkState())
    val state: StateFlow<HelaVitaLinkState> = _state.asStateFlow()

    private var job: Job? = null

    fun start(scope: CoroutineScope, platformId: Long) {
        if (job?.isActive == true) return
        job = scope.launch {
            when (val scan = vita3kLink.scan(platformId)) {
                Vita3kLink.Scan.Unreadable ->
                    notificationManager.showError(NotificationText.Res(R.string.hela_vita_link_unreadable))
                is Vita3kLink.Scan.Found -> {
                    val linked = scan.sure.count { vita3kLink.link(it.gameId, it.titleId) }
                    when {
                        linked > 0 -> notifyLinked(linked)
                        scan.sure.isNotEmpty() ->
                            notificationManager.showError(NotificationText.Res(R.string.hela_vita_link_failed))
                        scan.byTitle.isEmpty() ->
                            notificationManager.showSuccess(NotificationText.Res(R.string.hela_vita_link_none))
                    }
                    if (scan.byTitle.isNotEmpty()) {
                        _state.value = HelaVitaLinkState(
                            visible = true,
                            rows = scan.byTitle.map { HelaVitaLinkRow(it.gameId, it.gameTitle, it.titleId, it.appTitle) }
                        )
                    }
                }
            }
        }
    }

    fun moveFocus(delta: Int) {
        _state.update { it.copy(focusIndex = (it.focusIndex + delta).coerceIn(0, it.confirmIndex)) }
    }

    fun setFocus(index: Int) {
        _state.update { it.copy(focusIndex = index.coerceIn(0, it.confirmIndex)) }
    }

    fun moveHorizontal(delta: Int) {
        val current = _state.value
        when (current.focusIndex) {
            current.cancelIndex, current.confirmIndex -> setFocus(if (delta > 0) current.confirmIndex else current.cancelIndex)
            else -> current.rows.getOrNull(current.focusIndex)?.let { setChecked(it.gameId, delta > 0) }
        }
    }

    fun toggle(gameId: Long) {
        _state.update { it.copy(checked = if (gameId in it.checked) it.checked - gameId else it.checked + gameId) }
    }

    fun setChecked(gameId: Long, checked: Boolean) {
        _state.update { it.copy(checked = if (checked) it.checked + gameId else it.checked - gameId) }
    }

    fun activateFocused(scope: CoroutineScope) {
        val current = _state.value
        when (current.focusIndex) {
            current.cancelIndex -> dismiss()
            current.confirmIndex -> confirm(scope)
            else -> current.rows.getOrNull(current.focusIndex)?.let { toggle(it.gameId) }
        }
    }

    fun confirm(scope: CoroutineScope) {
        val current = _state.value
        val chosen = current.rows.filter { it.gameId in current.checked }
        dismiss()
        if (chosen.isEmpty()) return
        scope.launch {
            val linked = chosen.count { vita3kLink.link(it.gameId, it.titleId) }
            if (linked > 0) {
                notifyLinked(linked)
            } else {
                notificationManager.showError(NotificationText.Res(R.string.hela_vita_link_failed))
            }
        }
    }

    fun dismiss() {
        _state.value = HelaVitaLinkState()
    }

    private fun notifyLinked(count: Int) {
        notificationManager.showSuccess(NotificationText.Plural(R.plurals.hela_vita_link_done, count, listOf(count)))
    }
}
