package com.example.mychatroom.screen

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

// ---- UFO alien sprite sheets (res/drawable-nodpi), shared by Alien Shooter and Space Survivor ----
// alien_hover.png / alien_hit.png: 16-frame hover loop (normal / orange "hit" dome).
// alien_boom.png: 22-frame destruction.
// Every cell has the saucer's bottom-centre ("anchor") at the same spot, so the
// explosion lines up exactly with where the UFO was.
private const val ALIEN_ART_WIDTH = 44f        // UFO width in sheet pixels = alien size on screen
private const val HOVER_FRAMES = 16
private const val HOVER_CELL_W = 54
private const val HOVER_CELL_H = 41
private const val HOVER_ANCHOR_X = 27
private const val HOVER_ANCHOR_Y = 40
private const val HOVER_FPS = 12f              // 16 frames -> loops every ~1.3 s
private const val BOOM_FRAMES = 22
private const val BOOM_CELL_W = 78
private const val BOOM_CELL_H = 59
private const val BOOM_ANCHOR_X = 39
private const val BOOM_ANCHOR_Y = 58

/**
 * A hovering UFO centred on (centerX, centerY), [alienSize] px wide.
 * [animTime] (seconds) picks the frame; pass alien_hit.png as [sheet] to flash it orange.
 */
internal fun DrawScope.drawAlienHover(
    sheet: ImageBitmap, centerX: Float, centerY: Float, alienSize: Float, animTime: Float,
) {
    val scale = alienSize / ALIEN_ART_WIDTH
    val cell = (animTime * HOVER_FPS).toInt().mod(HOVER_FRAMES)
    drawCell(
        sheet, HOVER_CELL_W, HOVER_CELL_H, cell,
        left = centerX - HOVER_CELL_W * scale / 2f,
        top = centerY - HOVER_CELL_H * scale / 2f,
        scale = scale,
    )
}

/**
 * A UFO's destruction where it was (same centre and size as [drawAlienHover]).
 * [progress] 0..1 picks one of the 22 frames.
 */
internal fun DrawScope.drawAlienBoom(
    sheet: ImageBitmap, centerX: Float, centerY: Float, alienSize: Float, progress: Float,
) {
    val scale = alienSize / ALIEN_ART_WIDTH
    val cell = (progress * BOOM_FRAMES).toInt().coerceIn(0, BOOM_FRAMES - 1)
    // saucer anchor on screen, same as in the hover sheet
    val anchorX = centerX + (HOVER_ANCHOR_X - HOVER_CELL_W / 2f) * scale
    val anchorY = centerY + (HOVER_ANCHOR_Y - HOVER_CELL_H / 2f) * scale
    drawCell(
        sheet, BOOM_CELL_W, BOOM_CELL_H, cell,
        left = anchorX - BOOM_ANCHOR_X * scale,
        top = anchorY - BOOM_ANCHOR_Y * scale,
        scale = scale,
    )
}

/** Draws cell [index] of a one-row sheet with its top-left at (left, top), scaled by [scale]. */
private fun DrawScope.drawCell(
    sheet: ImageBitmap, cellW: Int, cellH: Int, index: Int, left: Float, top: Float, scale: Float,
) {
    drawImage(
        image = sheet,
        srcOffset = IntOffset(index * cellW, 0),
        srcSize = IntSize(cellW, cellH),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize((cellW * scale).roundToInt(), (cellH * scale).roundToInt()),
        filterQuality = FilterQuality.None,   // pixel art: crisp pixels, no blur
    )
}
