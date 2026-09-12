package com.example.gitloftandroid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CyberStarRating(
    rating: Int,
    onRatingChanged: ((Int) -> Unit)? = null,
    onRatingSelected: ((Int) -> Unit)? = onRatingChanged,
    modifier: Modifier = Modifier
) {
    val callback = onRatingChanged ?: onRatingSelected
    Row(
        modifier = modifier.padding(4.dp)
    ) {
        repeat(5) { index ->
            val isSelected = index < rating
            val icon = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline
            val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp)
                    .then(
                        if (callback != null) {
                            Modifier.clickable { callback(index + 1) }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
