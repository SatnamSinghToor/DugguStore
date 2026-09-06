package com.duggustore.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.duggustore.app.ui.theme.TextPrimary

/**
 * The app's one press-feedback style for the custom Box/Row/Column tap
 * targets used throughout (category tiles, chips, the add-to-cart button,
 * the quantity stepper, and so on). Plain Modifier.clickable{}'s default
 * ripple derives its colour from the local content colour, which on this
 * app's pastel/tinted surfaces comes out close to invisible — this pins a
 * dark ripple so a press reads the same way on every surface.
 */
@Composable
fun Modifier.dugguClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = rememberRipple(color = TextPrimary),
        onClick = onClick
    )
}
