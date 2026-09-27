package com.autodocs.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodocs.app.ui.theme.Accent
import com.autodocs.app.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * Легкі графіки на Canvas (етап 8). Правила з dataviz: одна серія — акцентний колір, лінія 2dp,
 * маркери ≥8dp з кільцем кольору підкладки 2dp, стовпчики ≤24dp з заокругленням 4dp лише на кінці
 * даних (біля нуля — прямо), сітка — тонкі приглушені лінії, підписи — кольором тексту.
 * Підказка по тапу: вибраний індекс віддається нагору, а екран показує значення над графіком.
 */

/** Колір «підкладки» картки для кілець навколо маркерів (скло поверх темного фону). */
val ChartSurface = Color(0xFF0E1726)
private val GridColor = Color(0x14FFFFFF)
private val CrosshairColor = Color(0x55FFFFFF)

private val axisStyle = TextStyle(color = TextSecondary, fontSize = 11.sp)

/** «Гарні» поділки осі: 0, 5 000, 10 000… */
internal fun niceTicks(minValue: Double, maxValue: Double, maxTicks: Int = 4): List<Double> {
    var lo = minValue
    var hi = maxValue
    if (hi - lo < 1e-9) { hi = lo + 1.0; lo -= 1.0 }
    val rough = (hi - lo) / maxTicks
    val mag = 10.0.pow(floor(log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * mag }.first { it >= rough }
    val start = floor(lo / step) * step
    val end = ceil(hi / step) * step
    val out = mutableListOf<Double>()
    var v = start
    while (v <= end + step / 2) { out += v; v += step }
    return out
}

/** Компактний підпис осі: 800 · 5 тис · 12,5 тис · 1,2 млн. */
fun compactNumber(v: Double): String {
    val a = abs(v)
    fun trim(x: Double): String {
        val r = Math.round(x * 10) / 10.0
        return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString().replace('.', ',')
    }
    return when {
        a >= 1_000_000 -> "${trim(v / 1_000_000)} млн"
        a >= 1_000 -> "${trim(v / 1_000)} тис"
        else -> trim(v)
    }
}

private fun DrawScope.yAxis(
    measurer: TextMeasurer,
    ticks: List<Double>,
    yOf: (Double) -> Float,
    plotLeft: Float,
    plotRight: Float,
    label: (Double) -> String
) {
    ticks.forEach { t ->
        val y = yOf(t)
        drawLine(GridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1.dp.toPx())
        val layout = measurer.measure(label(t), axisStyle)
        drawText(layout, topLeft = Offset(plotLeft - 6.dp.toPx() - layout.size.width, y - layout.size.height / 2f))
    }
}

private fun DrawScope.ringMarker(center: Offset, radius: Dp, color: Color) {
    drawCircle(ChartSurface, radius = radius.toPx() + 2.dp.toPx(), center = center)
    drawCircle(color, radius = radius.toPx(), center = center)
}

/**
 * Лінійний графік за часом. [xs] — дні (epochDay), [ys] — значення; [xTicks] — (день, підпис).
 * Тап вибирає найближчу по горизонталі точку.
 */
@Composable
fun TimeLineChart(
    xs: List<Long>,
    ys: List<Int>,
    xTicks: List<Pair<Long, String>>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    description: String = "Графік",
    yLabel: (Double) -> String = ::compactNumber
) {
    val measurer = rememberTextMeasurer()
    val yTicks = remember(ys) { if (ys.isEmpty()) emptyList() else niceTicks(ys.min().toDouble(), ys.max().toDouble()) }
    val xMin = (xs.minOrNull() ?: 0L).let { if (xs.size < 2 || xs.max() == it) it - 15 else it }
    val xMax = (xs.maxOrNull() ?: 0L).let { if (xs.size < 2 || it == xs.min()) it + 15 else it }
    val gutter = remember(yTicks) {
        (yTicks.maxOfOrNull { measurer.measure(yLabel(it), axisStyle).size.width } ?: 0)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(xs, gutter) {
                detectTapGestures { pos ->
                    if (xs.isEmpty()) return@detectTapGestures
                    val left = gutter + 10.dp.toPx()
                    val right = size.width - 10.dp.toPx()
                    val span = (xMax - xMin).toFloat().coerceAtLeast(1f)
                    val idx = xs.indices.minBy { abs(left + (xs[it] - xMin) / span * (right - left) - pos.x) }
                    onSelect(idx)
                }
            }
    ) {
        if (xs.isEmpty()) return@Canvas
        val left = gutter + 10.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val lo = yTicks.first()
        val hi = yTicks.last()
        val span = (xMax - xMin).toFloat().coerceAtLeast(1f)
        fun xOf(d: Long) = left + (d - xMin) / span * (right - left)
        fun yOf(v: Double) = (bottom - ((v - lo) / (hi - lo)).toFloat() * (bottom - top))

        yAxis(measurer, yTicks, ::yOf, left, right, yLabel)

        // Підписи часу під віссю (без накладань: пропускаємо, якщо не влазить).
        var lastEnd = Float.NEGATIVE_INFINITY
        xTicks.filter { it.first in xMin..xMax }.forEach { (d, text) ->
            val layout = measurer.measure(text, axisStyle)
            val x = (xOf(d) - layout.size.width / 2f).coerceIn(left - 4.dp.toPx(), size.width - layout.size.width.toFloat())
            if (x > lastEnd + 6.dp.toPx()) {
                drawText(layout, topLeft = Offset(x, bottom + 6.dp.toPx()))
                lastEnd = x + layout.size.width
            }
        }

        val pts = xs.indices.map { Offset(xOf(xs[it]), yOf(ys[it].toDouble())) }
        if (pts.size >= 2) {
            val line = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(pts.last().x, bottom)
                lineTo(pts.first().x, bottom)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(Accent.copy(alpha = 0.16f), Color.Transparent), startY = top, endY = bottom))
            drawPath(line, Accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        selectedIndex?.takeIf { it in pts.indices }?.let { i ->
            drawLine(CrosshairColor, Offset(pts[i].x, top), Offset(pts[i].x, bottom), strokeWidth = 1.dp.toPx())
        }
        val showAll = pts.size <= 24
        pts.forEachIndexed { i, p ->
            when {
                i == selectedIndex -> ringMarker(p, 5.dp, Accent)
                showAll || i == pts.lastIndex -> ringMarker(p, 4.dp, Accent)
            }
        }
    }
}

/**
 * Стовпчиковий графік однієї серії. Нульові значення — без стовпчика.
 * Вибраний стовпчик яскравий, решта — приглушені (коли є вибір).
 */
@Composable
fun SimpleBarChart(
    values: List<Double>,
    labels: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    description: String = "Графік",
    yLabel: (Double) -> String = ::compactNumber
) {
    val measurer = rememberTextMeasurer()
    val maxV = values.maxOrNull() ?: 0.0
    val yTicks = remember(values) { niceTicks(0.0, if (maxV > 0) maxV else 1.0, maxTicks = 3) }
    val gutter = remember(yTicks) { yTicks.maxOf { measurer.measure(yLabel(it), axisStyle).size.width } }
    val n = values.size

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(n, gutter) {
                detectTapGestures { pos ->
                    if (n == 0) return@detectTapGestures
                    val left = gutter + 10.dp.toPx()
                    val slot = (size.width - left) / n
                    val i = ((pos.x - left) / slot).toInt()
                    if (i in 0 until n) onSelect(i)
                }
            }
    ) {
        if (n == 0) return@Canvas
        val left = gutter + 10.dp.toPx()
        val right = size.width
        val top = 8.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val hi = yTicks.last()
        fun yOf(v: Double) = bottom - (v / hi).toFloat() * (bottom - top)

        yAxis(measurer, yTicks, ::yOf, left, right, yLabel)

        val slot = (right - left) / n
        val barW = min(24.dp.toPx(), max(2.dp.toPx(), slot - 4.dp.toPx()))
        val radius = min(4.dp.toPx(), barW / 2)
        // Підписи: якщо не влазять усі — кожен k-й.
        val labelW = labels.maxOfOrNull { measurer.measure(it, axisStyle).size.width } ?: 0
        val every = max(1, ceil((labelW + 4.dp.toPx()) / slot).toInt())

        values.forEachIndexed { i, v ->
            val cx = left + slot * i + slot / 2
            if (v > 0) {
                val yTop = min(yOf(v), bottom - 2.dp.toPx())
                val color = if (selectedIndex == null || selectedIndex == i) Accent else Accent.copy(alpha = 0.4f)
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = cx - barW / 2, top = yTop, right = cx + barW / 2, bottom = bottom,
                            topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
                            bottomLeftCornerRadius = CornerRadius.Zero, bottomRightCornerRadius = CornerRadius.Zero
                        )
                    )
                }
                drawPath(path, color)
            }
            if (i % every == 0 || i == selectedIndex) {
                val text = labels.getOrNull(i) ?: return@forEachIndexed
                val style = if (i == selectedIndex) axisStyle.copy(color = com.autodocs.app.ui.theme.TextPrimary) else axisStyle
                val layout = measurer.measure(text, style)
                drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, bottom + 6.dp.toPx()))
            }
        }
        drawLine(Color(0x33FFFFFF), Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())
    }
}
