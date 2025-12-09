package com.example.todoapp

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex

@Composable
fun <T> rememberDragDropState(
    items: List<T>,
    onMove: (Int, Int) -> Unit
): DragDropState<T> {
    val state = remember {
        DragDropState(items, onMove)
    }

    LaunchedEffect(items) {
        state.updateItems(items)
    }

    return state
}

class DragDropState<T>(
    var items: List<T>,
    private val onMove: (Int, Int) -> Unit
) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set
    var dragOffset by mutableStateOf(0f)
        private set
    var targetIndex by mutableStateOf<Int?>(null)
        private set

    private val itemHeights = mutableMapOf<Int, Float>()
    private var startIndex: Int? = null

    fun updateItems(newItems: List<T>) {
        items = newItems
    }

    fun setItemHeight(index: Int, height: Float) {
        itemHeights[index] = height
    }

    fun onDragStart(index: Int) {
        if (index < 0 || index >= items.size) return
        draggedIndex = index
        startIndex = index
        dragOffset = 0f
        targetIndex = index
    }

    fun onDrag(dragAmount: Float) {
        val currentIndex = draggedIndex
        if (currentIndex == null || currentIndex < 0 || currentIndex >= items.size) {
            onDragEnd()
            return
        }

        dragOffset += dragAmount

        val originalIndex = startIndex ?: return
        var calculatedTarget = originalIndex
        var accumulatedHeight = 0f

        if (dragOffset > 0) {
            for (i in (originalIndex + 1) until items.size) {
                val itemHeight = itemHeights[i] ?: continue
                if (dragOffset > accumulatedHeight + itemHeight / 2) {
                    accumulatedHeight += itemHeight
                    calculatedTarget = i
                } else {
                    break
                }
            }
        } else if (dragOffset < 0) {
            for (i in (originalIndex - 1) downTo 0) {
                val itemHeight = itemHeights[i] ?: continue
                if (dragOffset < -(accumulatedHeight + itemHeight / 2)) {
                    accumulatedHeight += itemHeight
                    calculatedTarget = i
                } else {
                    break
                }
            }
        }

        targetIndex = calculatedTarget
    }

    fun onDragEnd() {
        val originalIndex = startIndex
        val finalTarget = targetIndex

        if (originalIndex != null && finalTarget != null &&
            originalIndex >= 0 && originalIndex < items.size &&
            finalTarget >= 0 && finalTarget < items.size &&
            originalIndex != finalTarget) {
            onMove(originalIndex, finalTarget)
        }

        draggedIndex = null
        dragOffset = 0f
        startIndex = null
        targetIndex = null
    }
}

@Composable
fun <T> DraggableItem(
    index: Int,
    item: T,
    dragDropState: DragDropState<T>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(T, Boolean) -> Unit
) {
    if (index < 0 || index >= dragDropState.items.size) {
        Box(modifier = modifier) {
            content(item, false)
        }
        return
    }

    val isDragging = dragDropState.draggedIndex == index
    val isTarget = dragDropState.targetIndex == index && !isDragging
    val offset = if (isDragging) dragDropState.dragOffset else 0f
    var itemSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(itemSize.height) {
        if (itemSize.height > 0 && index < dragDropState.items.size) {
            dragDropState.setItemHeight(index, itemSize.height.toFloat())
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                itemSize = coordinates.size
            }
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = offset
                scaleX = if (isDragging) 0.95f else if (isTarget) 1.05f else 1f
                scaleY = if (isDragging) 0.95f else if (isTarget) 1.05f else 1f
                alpha = if (isDragging) 0.7f else 1f
            }
            .pointerInput(index, dragDropState) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragDropState.onDragStart(index)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDropState.onDrag(dragAmount.y)
                    },
                    onDragEnd = {
                        dragDropState.onDragEnd()
                    },
                    onDragCancel = {
                        dragDropState.onDragEnd()
                    }
                )
            }
    ) {
        content(item, isDragging)
    }
}