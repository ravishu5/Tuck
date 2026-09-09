package com.tuck.app.ui.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuck.app.R
import com.tuck.app.domain.model.Collection
import com.tuck.app.processing.ReminderPreset
import com.tuck.app.ui.components.ReminderDialog
import com.tuck.app.ui.components.getCollectionVisual
import com.tuck.app.ui.theme.TuckTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration

private const val TilesPerRow = 4
private val TileGap = 10.dp

/**
 * The sheet shown after a share lands: where does this go, and should it come back?
 *
 * Filing is presented as a grid of collection tiles rather than a scrolling strip of chips,
 * because filing is the whole reason the sheet is on screen. A horizontal strip shows three
 * collections and hides the rest behind a gesture nobody makes while holding a phone one-handed;
 * a grid shows a dozen at a glance and makes the choice a single tap.
 *
 * The item is already saved by the time this appears — nothing here is required, which is why
 * the primary button says "done" rather than "save".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaveToTuckSheet(
    state: ShareUiState.Saved,
    onToggleCollection: (Long) -> Unit,
    onCreateCollection: () -> Unit,
    onSetReminder: (Long?, String?) -> Unit,
    onOpenItem: (Long) -> Unit,
    onDone: () -> Unit
) {
    val colors = TuckTheme.colors
    val shapes = TuckTheme.shapes
    var reminderOpen by remember { mutableStateOf(false) }

    // Only the grid scrolls, and only once it is genuinely too tall — about three rows. The
    // sheet itself stays short, because it is covering something the reader was looking at.
    val maxGridHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.34f).coerceAtMost(300.dp)

    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text = stringResource(R.string.share_save_to_tuck),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(14.dp))

        // What is being saved, so the reader can tell they grabbed the right thing.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = shapes.medium,
                color = colors.accentContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.subtitle.isNotBlank()) {
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .heightIn(max = maxGridHeight)
                .verticalScroll(rememberScrollState())
        ) {
            // Rows of weighted tiles rather than a FlowRow of fixed widths. A fixed width
            // leaves the remainder as dead space on one side (53px left against 193px right
            // before this), and a width derived by division rounds up past the row and drops a
            // column. Weights divide the space exactly, whatever the screen.
            // A null entry is the "new collection" tile, which always comes last.
            val entries: List<Collection?> = state.collections + listOf(null)

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                entries.chunked(TilesPerRow).forEach { rowEntries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TileGap)
                    ) {
                        rowEntries.forEach { collection ->
                            if (collection == null) {
                                NewCollectionTile(
                                    onClick = onCreateCollection,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                CollectionTile(
                                    collection = collection,
                                    selected = state.selectedCollectionIds.contains(collection.id),
                                    onClick = { onToggleCollection(collection.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        // Keeps a short final row's tiles the same size as a full row's, rather
                        // than stretching two tiles across the whole width.
                        repeat(TilesPerRow - rowEntries.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        ReminderRow(
            remindAt = state.remindAt,
            onOpen = { reminderOpen = true },
            onClear = { onSetReminder(null, null) }
        )

        if (reminderOpen) {
            ReminderDialog(
                initialRemindAt = state.remindAt,
                initialNote = null,
                onDismiss = { reminderOpen = false },
                onSave = { at, note ->
                    onSetReminder(at, note)
                    reminderOpen = false
                },
                onClear = {
                    onSetReminder(null, null)
                    reminderOpen = false
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onOpenItem(state.savedItemId) },
                shape = shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceCard,
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.share_open_in_tuck), fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onDone,
                shape = shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textOnAccent
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .height(50.dp)
            ) {
                Text(
                    text = if (state.selectedCollectionIds.isEmpty()) {
                        stringResource(R.string.share_quick_save)
                    } else {
                        stringResource(R.string.collections_done)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * One collection as a tile: its colour, its icon, its name.
 *
 * Selection is a ring and a tick rather than a colour change, so the tile keeps the colour the
 * reader recognises it by while still reading as chosen.
 */
@Composable
private fun CollectionTile(
    collection: Collection,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TuckTheme.colors
    // The app already knows how to turn a collection into a colour and an emoji — the stored
    // icon is a Material name like `picture_as_pdf`, which is not something to render as text.
    val visual = getCollectionVisual(
        name = collection.name,
        iconHint = collection.icon,
        colorHint = collection.color,
        collectionId = collection.id
    )

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(visual.color.copy(alpha = if (selected) 1f else 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = visual.emoji,
                style = MaterialTheme.typography.headlineSmall
            )

            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = colors.accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.textOnAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = collection.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NewCollectionTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TuckTheme.colors

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.share_new),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** A single line stating the reminder, which opens the shared dialog to change it. */
@Composable
private fun ReminderRow(
    remindAt: Long?,
    onOpen: () -> Unit,
    onClear: () -> Unit
) {
    val colors = TuckTheme.colors
    val shapes = TuckTheme.shapes

    Surface(
        shape = shapes.medium,
        color = if (remindAt != null) colors.accentContainer else colors.surfaceCard,
        border = BorderStroke(
            1.dp,
            if (remindAt != null) colors.accent.copy(alpha = 0.5f) else colors.border
        ),
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Alarm,
                contentDescription = null,
                tint = if (remindAt != null) colors.accent else colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (remindAt != null) {
                    android.text.format.DateUtils.getRelativeTimeSpanString(
                        remindAt,
                        System.currentTimeMillis(),
                        android.text.format.DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                } else {
                    stringResource(R.string.detail_remind_me)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (remindAt != null) colors.accent else colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            if (remindAt != null) {
                Text(
                    text = stringResource(R.string.reminder_clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                    modifier = Modifier.clickable(onClick = onClear)
                )
            }
        }
    }
}
