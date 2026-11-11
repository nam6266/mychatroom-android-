package com.example.mychatroom.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/** The app's signature violet -> pink gradient. */
@Composable
fun brandGradient(): Brush = Brush.linearGradient(
    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
)

/**
 * Full-screen backdrop: a soft vertical gradient with two blurred "glows"
 * (violet top-left, pink bottom-right). Works in both light and dark themes.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bottom = colors.surfaceVariant.copy(alpha = 0.7f).compositeOver(colors.background)
    val glowTop = colors.primary.copy(alpha = 0.22f)
    val glowBottom = colors.secondary.copy(alpha = 0.16f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, bottom)))
            .drawBehind {
                val topCenter = Offset(size.width * 0.1f, size.height * 0.05f)
                val topRadius = size.width * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowTop, Color.Transparent),
                        center = topCenter,
                        radius = topRadius
                    ),
                    radius = topRadius,
                    center = topCenter
                )
                val bottomCenter = Offset(size.width * 0.95f, size.height * 0.85f)
                val bottomRadius = size.width * 0.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowBottom, Color.Transparent),
                        center = bottomCenter,
                        radius = bottomRadius
                    ),
                    radius = bottomRadius,
                    center = bottomCenter
                )
            },
        content = content
    )
}

/** Round avatar showing the first letter of [name] on the brand gradient. */
@Composable
fun GradientAvatar(name: String, size: Dp, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brandGradient()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

/** Title text painted with the brand gradient. */
@Composable
fun GradientTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(brush = brandGradient(), fontWeight = FontWeight.ExtraBold))
    )
}
