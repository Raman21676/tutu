package com.nova.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nova.browser.ui.theme.CoralRed
import com.nova.browser.ui.theme.NovaTheme

@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    title: String = "",
    subtitle: String? = null,
    checkedColor: Color = CoralRed,
    uncheckedColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "switch_scale"
    )
    
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "thumb_position"
    )
    
    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedColor else uncheckedColor,
        label = "track_color"
    )
    
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface 
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
                    )
                }
            }
        }
        
        // Switch
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .scale(scale)
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled) trackColor else uncheckedColor.copy(alpha = 0.5f))
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = thumbPosition.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (enabled) 1f else 0.5f))
            )
        }
    }
}

@Preview
@Composable
fun ToggleSwitchOnPreview() {
    NovaTheme {
        ToggleSwitch(
            checked = true,
            onCheckedChange = {},
            title = "Desktop Mode"
        )
    }
}

@Preview
@Composable
fun ToggleSwitchOffPreview() {
    NovaTheme {
        ToggleSwitch(
            checked = false,
            onCheckedChange = {},
            title = "Fullscreen"
        )
    }
}
