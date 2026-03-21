package com.tutu.browser.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.tutu.browser.ui.theme.CoralRed
import com.tutu.browser.ui.theme.TutuTheme

@Composable
fun TextLogo(
    text: String = "tutu",
    color: Color = CoralRed,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            color = color,
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier
    )
}

@Preview
@Composable
fun TextLogoPreview() {
    TutuTheme {
        TextLogo()
    }
}
