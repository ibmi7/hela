package com.nendo.argosy.ui.hela.vita

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nendo.argosy.R
import com.nendo.argosy.ui.components.FocusedScroll
import com.nendo.argosy.ui.components.Modal
import com.nendo.argosy.ui.components.SwitchPreference
import com.nendo.argosy.ui.primitives.ModalActionButton
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme

@Composable
fun HelaVitaLinkModal(
    state: HelaVitaLinkState,
    onToggle: (Long, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalArgosyTheme.current
    Modal(
        title = stringResource(R.string.hela_vita_link_modal_title),
        subtitle = stringResource(R.string.hela_vita_link_modal_subtitle),
        onDismiss = onDismiss
    ) {
        val listState = rememberLazyListState()
        FocusedScroll(listState = listState, focusedIndex = state.focusIndex.coerceAtMost(state.rows.lastIndex))
        LazyColumn(state = listState, modifier = Modifier.weight(1f, fill = false)) {
            itemsIndexed(state.rows, key = { _, row -> row.gameId }) { index, row ->
                SwitchPreference(
                    title = row.gameTitle,
                    subtitle = stringResource(
                        R.string.hela_vita_link_modal_row_subtitle,
                        row.appTitle ?: row.titleId,
                        row.titleId
                    ),
                    isEnabled = row.gameId in state.checked,
                    isFocused = state.focusIndex == index,
                    onToggle = { onToggle(row.gameId, it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.spacingMd))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm, Alignment.End)
        ) {
            ModalActionButton(
                label = stringResource(R.string.hela_vita_link_modal_cancel),
                tint = theme.focusAccent,
                restLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focused = state.focusIndex == state.cancelIndex,
                onClick = onDismiss
            )
            ModalActionButton(
                label = stringResource(R.string.hela_vita_link_modal_confirm),
                tint = theme.focusAccent,
                restLabelColor = MaterialTheme.colorScheme.onSurface,
                focused = state.focusIndex == state.confirmIndex,
                onClick = onConfirm,
                enabled = state.checked.isNotEmpty()
            )
        }
    }
}
