package org.example.animation.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.example.animation.model.AnimationProject
import org.example.animation.io.encodeImage

object ExportManager {
    
    /**
     * Рендерит указанный кадр проекта в PNG байты.
     */
    fun exportFrameToPng(
        project: AnimationProject, 
        frameIndex: Int, 
        width: Int, 
        height: Int,
        density: Density
    ): ByteArray {
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
        val canvasDrawScope = CanvasDrawScope()
        
        canvasDrawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            // Масштабируем отрисовку под целевой размер экспорта
            val scaleX = width.toFloat() / project.canvasWidth.toFloat()
            val scaleY = height.toFloat() / project.canvasHeight.toFloat()
            
            this.scale(scaleX, scaleY, Offset.Zero) {
                Renderer.renderFrame(this, project, frameIndex)
            }
        }
        
        return encodeImage(bitmap)
    }

    /**
     * Экспорт всех кадров
     */
    fun exportSequenceToPngs(
        project: AnimationProject,
        width: Int,
        height: Int,
        density: Density,
        onFrame: (Int, ByteArray) -> Unit
    ) {
        for (i in 0 until project.maxFrames) {
            val data = exportFrameToPng(project, i, width, height, density)
            onFrame(i, data)
        }
    }
}
