package com.duggustore.app.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/** Screen-to-screen nav (NavGraph's NavHost) uses this exact timing — every
 * other step-by-step AnimatedContent in the app (signup, welcome, seller and
 * delivery onboarding) shares this instead of its own default-spring spec,
 * so the whole app slides at one consistent speed rather than several.
 */
const val AppSlideDurationMs = 300

fun <S> slideStepTransition(movingForward: Boolean): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    val duration = tween<Float>(AppSlideDurationMs)
    if (movingForward) {
        (slideInHorizontally(tween(AppSlideDurationMs)) { it } + fadeIn(duration)) togetherWith
            (slideOutHorizontally(tween(AppSlideDurationMs)) { -it } + fadeOut(duration))
    } else {
        (slideInHorizontally(tween(AppSlideDurationMs)) { -it } + fadeIn(duration)) togetherWith
            (slideOutHorizontally(tween(AppSlideDurationMs)) { it } + fadeOut(duration))
    }
}
