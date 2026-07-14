package org.example.animation.io

import androidx.compose.ui.geometry.Offset
import org.example.animation.model.AnimationProject

/**
 * Flood fill реализация для Android
 * TODO: Реализовать с использованием Android Canvas
 */
actual fun floodFillOnBitmap(
    project: AnimationProject,
    layerIndex: Int,
    frameIndex: Int,
    point: Offset,
    fillColor: ULong
) {
    // TODO: сделать через Android Bitmap и Canvas
}

/**
 * Пипетка - получение цвета из bitmap для Android
 * TODO: Реализовать с использованием Android Canvas
 */
actual fun pickColorFromBitmap(
    project: AnimationProject,
    point: Offset,
    layerIndex: Int,
    frameIndex: Int
): ULong? {
    // TODO: сделать через Android Bitmap
    return null
}