package org.example.animation.io

import androidx.compose.ui.geometry.Offset
import org.example.animation.model.AnimationProject

/**
 * Flood fill реализация для Android (заглушка)
 */
actual fun floodFillOnBitmap(
    project: AnimationProject,
    layerIndex: Int,
    frameIndex: Int,
    point: Offset,
    fillColor: ULong
) {
    // TODO: Android-специфичная реализация с использованием Android Canvas
}

/**
 * Пипетка - получение цвета из bitmap для Android (заглушка)
 */
actual fun pickColorFromBitmap(
    project: AnimationProject,
    point: Offset
): ULong? {
    // TODO: Android-специфичная реализация
    return null
}