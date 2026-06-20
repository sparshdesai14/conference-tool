package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TickerBar(
    activeBookings: List<BookingEntity>,
    scheduledBookings: List<BookingEntity>,
    viewModel: com.example.viewmodel.BookingViewModel
) {
    val gyaanActive = activeBookings.firstOrNull { it.roomName.lowercase() == "gyaan" }
    val dharmaActive = activeBookings.firstOrNull { it.roomName.lowercase() == "dharma" }

    val hasGyaan = gyaanActive != null
    val hasDharma = dharmaActive != null

    // Determine overall background color and text
    val (backgroundColor, textColor, tickerHtmlText) = when {
        !hasGyaan && !hasDharma -> {
            Triple(
                SleekPrimary,
                Color.White,
                "Both conference rooms are free — book your slot before heading in"
            )
        }
        hasGyaan && hasDharma -> {
            Triple(
                StatusBusyRed,
                Color.White,
                "⚠ ALL ROOMS OCCUPIED  •  Gyaan: ${gyaanActive?.department}  |  Dharma: ${dharmaActive?.department}"
            )
        }
        else -> {
            val occupiedRoom = if (hasGyaan) "Gyaan" else "Dharma"
            val occupiedDept = if (hasGyaan) gyaanActive?.department else dharmaActive?.department
            val freeRoom = if (hasGyaan) "Dharma" else "Gyaan"
            Triple(
                StatusSchedAmber,
                Color.White,
                "⚠ $occupiedRoom occupied by $occupiedDept  •  $freeRoom is free — book now"
            )
        }
    }

    // Infinite blinking animation for occupied dots
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp)
            .testTag("ticker_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot indicator
        val dotColor = when {
            !hasGyaan && !hasDharma -> Color(0xFF5BE38A)
            hasGyaan && hasDharma -> StatusBusyRedBg
            else -> StatusSchedAmberBorder
        }

        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (hasGyaan || hasDharma) alphaAnim else 1.0f)
                .clip(CircleShape)
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Marquee Text
        Text(
            text = tickerHtmlText,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = 45.dp
                )
                .testTag("ticker_text")
        )
    }
}
