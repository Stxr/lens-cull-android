package com.stxr.lenscull.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RatingBar(
  rating: Int,
  onRating: (Int) -> Unit,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  Row(modifier) {
    (1..5).forEach { star ->
      IconButton(
        onClick = { onRating(if (rating == star) 0 else star) },
        modifier = Modifier.size(if (compact) 30.dp else 42.dp),
      ) {
        Icon(
          imageVector = if (star <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
          contentDescription = "$star 星",
          tint = if (star <= rating) Color(0xFFFFC857) else Color(0xFFB5B5B5),
          modifier = Modifier.size(if (compact) 18.dp else 26.dp),
        )
      }
    }
  }
}
