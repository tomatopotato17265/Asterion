package dev.tomatopotato.asterion.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.tomatopotato.asterion.R
import dev.tomatopotato.asterion.tabs.AppTab
import dev.tomatopotato.asterion.tabs.TabLayout
import dev.tomatopotato.asterion.viewmodel.TabsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    tabs: TabsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var order by remember {
        val current = tabs.freeOrder
        val rest = tabs.freeCandidates.filter { it !in current }
        mutableStateOf(current + rest)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        ReorderableTabList(
            order = order,
            onReorder = { newOrder ->
                order = newOrder
                tabs.updateFreeOrder(newOrder)
            },
        )

        Text(
            text = "Drag to reorder. The top ${TabLayout.freeSlotCount} tabs appear in your tab bar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReorderableTabList(
    order: List<AppTab>,
    onReorder: (List<AppTab>) -> Unit,
) {
    val itemHeight = 56.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    var draggedTab by remember { mutableStateOf<AppTab?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val latestOrder = rememberUpdatedState(order)

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            order.forEachIndexed { index, tab ->
                key(tab) {
                    if (index > 0) HorizontalDivider()

                    val isDragged = draggedTab == tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                            .pointerInput(tab) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedTab = tab
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        draggedTab = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggedTab = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y

                                        val currentOrder = latestOrder.value
                                        val current = currentOrder.indexOf(tab)
                                        if (current < 0) return@detectDragGesturesAfterLongPress
                                        val target = (current + (dragOffset / itemHeightPx).roundToInt())
                                            .coerceIn(0, currentOrder.lastIndex)
                                        if (target != current) {
                                            val moved = currentOrder.toMutableList()
                                            moved.add(target, moved.removeAt(current))
                                            dragOffset -= (target - current) * itemHeightPx
                                            onReorder(moved)
                                        }
                                    },
                                )
                            }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            tint = if (index < TabLayout.freeSlotCount) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = tab.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_drag_handle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
