package com.nendo.argosy.ui.hela.vita

import com.nendo.argosy.data.emulator.Vita3kLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class HelaVitaLinkController @Inject constructor(
    private val vita3kLink: Vita3kLink
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope, platformId: Long) {
        if (job?.isActive == true) return
        job = scope.launch { vita3kLink.scan(platformId) }
    }
}
