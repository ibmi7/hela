package com.nendo.argosy.ui.hela.vita

import com.nendo.argosy.R
import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.showError
import com.nendo.argosy.core.notification.showSuccess
import com.nendo.argosy.data.emulator.Vita3kLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class HelaVitaLinkController @Inject constructor(
    private val vita3kLink: Vita3kLink,
    private val notificationManager: NotificationManager
) {
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
                        linked > 0 -> notificationManager.showSuccess(
                            NotificationText.Plural(R.plurals.hela_vita_link_done, linked, listOf(linked))
                        )
                        scan.sure.isNotEmpty() ->
                            notificationManager.showError(NotificationText.Res(R.string.hela_vita_link_failed))
                        scan.byTitle.isEmpty() ->
                            notificationManager.showSuccess(NotificationText.Res(R.string.hela_vita_link_none))
                    }
                }
            }
        }
    }
}
