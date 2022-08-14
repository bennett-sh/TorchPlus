package com.bnsh.torchplus.ui.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Partially based on https://github.com/elec60/ComposeSlider/blob/e0aec1771c35965aa733d6b673c7d6363a7caa02/app/src/main/java/com/hashem/mousavi/composeslider/MainActivity.kt
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    activeHeight: Dp = 48.dp,
    inactiveHeight: Dp = 4.dp,
    onDragEnd: (Float) -> Unit = {  },
    onDragStart: (Float) -> Unit = {  },
    icon: (Float) -> ImageVector? = { null },
    modifier: Modifier = Modifier
) {

    var iconVector by remember { mutableStateOf(icon(value)) }

    var touchX by remember { mutableStateOf(0f) }

    var offsetLeft by remember { mutableStateOf(0f) }
    var offsetRight by remember { mutableStateOf(0f) }

    val sliderBackground = MaterialTheme.colorScheme.onPrimaryContainer
    val onSliderBackground = MaterialTheme.colorScheme.background
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    var iconPainter: VectorPainter? = null

    if(iconVector != null) {
        iconPainter = icon(value)?.let { rememberVectorPainter(image = it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(activeHeight)
            .pointerInteropFilter { event ->
                touchX = event.x

                if(touchX <= 0) touchX = 0f
                if(touchX >= offsetRight - offsetLeft) touchX = offsetRight - offsetLeft

                onValueChange(touchX / (offsetRight - offsetLeft))

                if(event.action == MotionEvent.ACTION_UP) {
                    onDragEnd(touchX / (offsetRight - offsetLeft))
                }

                if(event.action == MotionEvent.ACTION_DOWN) {
                    onDragStart(touchX / (offsetRight - offsetLeft))
                }

                return@pointerInteropFilter event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_DOWN
            }
            .onGloballyPositioned { pos ->
                val winBounds = pos.boundsInWindow()

                offsetLeft = winBounds.left
                offsetRight = winBounds.right
            }
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val rect = RoundRect(
                0f,
                0f,
                size.width,
                activeHeight.toPx(),
                cornerRadius = CornerRadius(activeHeight.toPx() / 2f, activeHeight.toPx() / 2f)
            )

            val path = Path()
            path.addRoundRect(rect)

            drawRoundRect(
                size = size.copy(height = inactiveHeight.toPx()),
                topLeft = Offset(0f, (activeHeight / 2).toPx()),
                color = trackColor,
                cornerRadius = CornerRadius(activeHeight.toPx() / 2f, activeHeight.toPx() / 2f)
            )

            clipPath(path = path) {
                var x = (offsetRight - offsetLeft) * value

                if(x < activeHeight.toPx()) {
                    x = activeHeight.toPx()
                }

                drawRoundRect(
                    color = sliderBackground,
                    cornerRadius = CornerRadius(activeHeight.toPx() / 2f, activeHeight.toPx() / 2f),
                    size = Size(x, activeHeight.toPx())
                )

                iconVector = icon(touchX / size.width)

                if(iconPainter != null) {
                    translate(
                        left = x - (activeHeight.toPx() / 2) - (iconPainter.intrinsicSize.width / 2),
                        top = (activeHeight / 2).toPx() - iconPainter.intrinsicSize.height / 2
                    ) {
                        with(iconPainter) {
                            draw(
                                iconPainter.intrinsicSize,
                                colorFilter = ColorFilter.tint(onSliderBackground)
                            )
                        }
                    }
                }
            }
        }
    }
}
