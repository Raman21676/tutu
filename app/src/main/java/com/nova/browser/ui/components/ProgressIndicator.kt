package com.nova.browser.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.theme.TutuTheme

@Composable
fun LinearProgressIndicatorCustom(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(3.dp)
                .background(CoralRed)
        )
    }
}

@Composable
fun IndeterminateProgressIndicator(
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp),
        color = CoralRed,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Preview
@Composable
fun ProgressIndicatorPreview() {
    TutuTheme {
        LinearProgressIndicatorCustom(
            progress = 0.6f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
fun IndeterminateProgressIndicatorPreview() {
    TutuTheme {
        IndeterminateProgressIndicator(
            modifier = Modifier.fillMaxWidth()
        )
    }
}
