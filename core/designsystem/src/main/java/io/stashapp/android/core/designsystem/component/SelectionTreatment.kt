package io.stashapp.android.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.SpineColors

internal data class SelectionTreatment(
    val background: Color,
    val border: Color,
    val content: Color,
    val weight: FontWeight,
)

@Composable
internal fun spineSelectionTreatment(selected: Boolean): SelectionTreatment {
    val accent = LocalAccentColors.current
    return if (selected) {
        SelectionTreatment(
            background = accent.primary.copy(alpha = 0.12f),
            border = accent.primary.copy(alpha = 0.45f),
            content = accent.primary,
            weight = FontWeight.SemiBold,
        )
    } else {
        SelectionTreatment(
            background = SpineColors.Surface,
            border = SpineColors.Border,
            content = SpineColors.OnSurfaceVariant,
            weight = FontWeight.Normal,
        )
    }
}
