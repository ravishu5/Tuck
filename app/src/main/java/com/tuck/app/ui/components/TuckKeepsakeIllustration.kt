package com.tuck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tuck.app.ui.theme.TuckTheme

/** A small stack of keepsakes, drawn locally so every theme and offline state feels intentional. */
@Composable
fun TuckKeepsakeIllustration(icon: ImageVector, modifier: Modifier = Modifier) {
    val colors = TuckTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(modifier = modifier.size(148.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(136.dp).background(colors.accentContainer, CircleShape))
        Box(
            Modifier.size(82.dp, 102.dp).rotate(-16f)
                .background(colors.surfaceVariant, shape)
                .border(1.dp, colors.border, shape)
        )
        Box(
            Modifier.size(82.dp, 102.dp).rotate(10f)
                .shadow(6.dp, shape, ambientColor = colors.accent.copy(alpha = 0.12f),
                    spotColor = colors.accent.copy(alpha = 0.12f))
                .background(colors.surface, shape)
                .border(1.dp, colors.borderSubtle, shape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
                Box(Modifier.width(40.dp).height(4.dp).background(colors.surfaceVariant, CircleShape))
                Spacer(Modifier.height(5.dp))
                Box(Modifier.width(26.dp).height(4.dp).background(colors.surfaceVariant, CircleShape))
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(10.dp).background(colors.accent, CircleShape))
    }
}
