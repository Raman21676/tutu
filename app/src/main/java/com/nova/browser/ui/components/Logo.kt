package com.nova.browser.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nova.browser.R
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.theme.NovaTheme

@Composable
fun NovaLogo(
    modifier: Modifier = Modifier,
    size: Int = 120,
    color: Color = CoralRed,
    isPressed: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "logo_scale"
    )
    
    Box(
        modifier = modifier
            .size(size.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Since we don't have actual vector assets yet, we'll use text-based logo
        // In production, replace with actual dodo bird vector graphic
        TextLogo(
            text = "nova",
            color = color
        )
    }
}

@Composable
private fun TextLogo(
    text: String = "nova",
    color: Color = CoralRed
) {
    androidx.compose.material3.Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            color = color,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    )
}

@Preview
@Composable
fun NovaLogoPreview() {
    NovaTheme {
        NovaLogo()
    }
}
