package com.tutu.browser.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tutu.browser.R
import com.tutu.browser.ui.theme.TutuTheme

@Composable
fun AppIcon(
    modifier: Modifier = Modifier,
    size: Int = 120,
    isPressed: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "icon_scale"
    )
    
    Image(
        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
        contentDescription = "Tutu Logo",
        modifier = modifier
            .size(size.dp)
            .scale(scale)
    )
}

@Preview
@Composable
fun AppIconPreview() {
    TutuTheme {
        AppIcon()
    }
}
