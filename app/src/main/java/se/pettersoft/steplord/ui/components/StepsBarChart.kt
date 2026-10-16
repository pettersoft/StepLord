package se.pettersoft.steplord.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

data class BarEntry(val label: String, val value: Int)

// Labels are stored in the chart model together with the values, so both always change at the
// same time. A separate label list can briefly disagree with the model while it updates (e.g. 7
// labels for a month of values), and Vico fails on missing labels.
private val LabelsKey = ExtraStore.Key<List<String>>()

// Smallest top of the y-axis, so days with few or no steps still get whole-number labels
private const val MIN_AXIS_MAX = 1_000.0

// Above this many bars (a month), use thinner bars and label every 7th day
private const val MAX_BARS_FOR_ALL_LABELS = 7
private const val LABEL_SPACING_MANY_BARS = 7

/** Y-axis from 0 to the largest value rounded up to two significant digits, at least [MIN_AXIS_MAX]. */
internal object StepsRangeProvider : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore) = 0.0

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val value = max(maxY, MIN_AXIS_MAX)
        val base = 10.0.pow(floor(log10(value)) - 1)
        return ceil(value / base) * base
    }
}

/** Bar chart of daily steps. Always fits all [entries] in the available width, without scrolling. */
@Composable
fun StepsBarChart(entries: List<BarEntry>, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(entries) {
        if (entries.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries { series(entries.map { it.value }) }
                extras { it[LabelsKey] = entries.map(BarEntry::label) }
            }
        }
    }
    val manyBars = entries.size > MAX_BARS_FOR_ALL_LABELS
    val startFormatter = remember {
        val numberFormat = NumberFormat.getIntegerInstance()
        CartesianValueFormatter { _, y, _ -> numberFormat.format(y) }
    }
    val bottomFormatter = remember {
        CartesianValueFormatter { context, x, _ ->
            val index = x.toInt()
            // Never blank: Vico throws on blank labels
            context.model.extraStore.getOrNull(LabelsKey)?.getOrNull(index) ?: (index + 1).toString()
        }
    }
    val barColor = MaterialTheme.colorScheme.primary
    val column = remember(barColor, manyBars) {
        val thickness = if (manyBars) 6.dp else 16.dp
        LineComponent(Fill(barColor), thickness, RoundedCornerShape(topStart = thickness / 3, topEnd = thickness / 3))
    }
    val theme = rememberM3VicoTheme(
        columnCartesianLayerColors = listOf(barColor),
        lineColor = MaterialTheme.colorScheme.outlineVariant,
        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ProvideVicoTheme(theme) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(column),
                    columnCollectionSpacing = if (manyBars) 4.dp else 24.dp,
                    rangeProvider = StepsRangeProvider,
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = startFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomFormatter,
                    itemPlacer = remember(manyBars) {
                        HorizontalAxis.ItemPlacer.aligned(spacing = { if (manyBars) LABEL_SPACING_MANY_BARS else 1 })
                    },
                ),
            ),
            modelProducer = modelProducer,
            modifier = modifier,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content, minZoom = Zoom.Content),
        )
    }
}
