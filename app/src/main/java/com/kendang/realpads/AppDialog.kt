package com.kendang.realpads

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val DIALOG_MAX_WIDTH = 380.dp
private val DIALOG_EDGE_MARGIN = 16.dp      // jarak minimum kartu ke tepi layar (kiri & kanan sama)
val DIALOG_PAD_H = 24.dp                    // padding isi kiri = kanan (SIMETRIS); bar gulir di tengah jalur ini
private const val DIALOG_MAX_HEIGHT_FRACTION = 0.92f

/**
 * Dialog standar SEMUA dialog di app (menggantikan AlertDialog / Dialog buatan sendiri).
 *
 * - Lebar kartu menyesuaikan layar; padding isi selalu kiri = kanan (tidak ada lagi sisi yang lebih sempit).
 * - Tinggi kartu dibatasi ke tinggi layar (landscape pendek pun aman). Judul dan tombol bawah SELALU terlihat,
 *   hanya bagian isi di tengah yang digulir.
 * - Bar gulir muncul otomatis HANYA bila isi lebih panjang dari ruang yang ada; tipis, di tengah jalur kanan,
 *   bisa diseret, plus pudar halus di tepi atas/bawah isi.
 *
 * [scrollBody] = true (default): [content] digulir oleh dialog ini. JANGAN taruh verticalScroll / LazyColumn di dalamnya.
 * [scrollBody] = false: [content] tidak digulir dialog (mendapat tinggi terbatas) - untuk isi yang punya penggulirnya
 *   sendiri (LazyColumn, tab dengan gulir masing-masing). Pasang [gutterScrollbar] di penggulir itu agar bar-nya
 *   tampil di jalur yang sama.
 * [dismissable] = false: tap di luar dan tombol back diabaikan (dialog yang wajib memilih tombol).
 * [buttons]: isi baris tombol bawah; pakai [DialogButton] (lebar dibagi rata).
 */
@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissable: Boolean = true,
    scrollBody: Boolean = true,
    maxWidth: Dp = DIALOG_MAX_WIDTH,
    subtitle: String? = null,
    titleLeading: (@Composable () -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val cfg = LocalConfiguration.current
    val cardWidth = minOf(maxWidth.value, cfg.screenWidthDp - DIALOG_EDGE_MARGIN.value * 2).dp
    val cardMaxHeight = (cfg.screenHeightDp * DIALOG_MAX_HEIGHT_FRACTION).dp

    Dialog(
        onDismissRequest = { if (dismissable) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = dismissable, dismissOnClickOutside = dismissable)
    ) {
        Column(
            modifier = Modifier
                .width(cardWidth)
                .heightIn(max = cardMaxHeight)
                .clip(RoundedCornerShape(28.dp))
                .background(PanelColor)
                .padding(top = 24.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DIALOG_PAD_H),
                verticalAlignment = Alignment.CenterVertically
            ) {
                titleLeading?.invoke()
                Text(
                    title,
                    color = PadTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                titleTrailing?.invoke()
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = TextDim,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DIALOG_PAD_H).padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            if (scrollBody) {
                DialogScrollBody(content)
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .padding(horizontal = DIALOG_PAD_H)
                ) { content() }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DIALOG_PAD_H),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { buttons() }
        }
    }
}

/** Tombol baris bawah dialog: lebar dibagi rata. Tanpa [container] = tombol netral (BATAL/TUTUP); aksi utama pakai warna tema. */
@Composable
fun RowScope.DialogButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true,
    container: Color = if (primary) ThemeState.accentColor else CtrlPanelColor,
    contentColor: Color = if (primary) ThemeState.accentTextColor else PadTextColor
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = contentColor)
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1) }
}

// ---------------------------------------------------------------------------------------------
// Penggulir bawaan dialog (scrollBody = true)
// ---------------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.DialogScrollBody(content: @Composable ColumnScope.() -> Unit) {
    val state = rememberScrollState()
    var viewH by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .onSizeChanged { viewH = it.height }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state)
                .padding(horizontal = DIALOG_PAD_H)
        ) { content() }
        if (state.canScrollBackward) {
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(16.dp)
                    .background(Brush.verticalGradient(listOf(PanelColor, Color.Transparent)))
            )
        }
        if (state.canScrollForward) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(16.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, PanelColor)))
            )
        }
        DialogScrollbar(state, viewH)
    }
}

private fun thumbHeight(trackH: Float, viewH: Int, max: Int, minPx: Float): Float =
    (trackH * viewH / (viewH + max)).coerceAtLeast(minPx).coerceAtMost(trackH)

/** Bar gulir 3dp tepat di TENGAH jalur padding kanan, jarak 6dp dari atas/bawah, bisa diseret. */
@Composable
private fun BoxScope.DialogScrollbar(state: ScrollState, viewH: Int) {
    val max = state.maxValue
    if (viewH <= 0 || max <= 0 || max == Int.MAX_VALUE) return
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(DIALOG_PAD_H)
            .fillMaxHeight()
            .pointerInput(max, viewH) {
                detectVerticalDragGestures { change, dy ->
                    change.consume()
                    val trackH = size.height - 12.dp.toPx()
                    val travel = (trackH - thumbHeight(trackH, size.height, state.maxValue, 36.dp.toPx())).coerceAtLeast(1f)
                    state.dispatchRawDelta(dy * state.maxValue / travel)
                }
            }
            .drawBehind {
                val inset = 6.dp.toPx()
                val trackH = size.height - inset * 2
                val thumbH = thumbHeight(trackH, size.height.toInt(), state.maxValue, 36.dp.toPx())
                val frac = state.value.toFloat() / state.maxValue.coerceAtLeast(1)
                val w = 3.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.38f),
                    topLeft = Offset((size.width - w) / 2f, inset + (trackH - thumbH) * frac),
                    size = Size(w, thumbH),
                    cornerRadius = CornerRadius(w / 2f)
                )
            }
    )
}

// ---------------------------------------------------------------------------------------------
// Bar gulir untuk penggulir di dalam dialog (scrollBody = false): digambar di jalur padding kanan dialog
// (di luar tepi penggulir), jadi posisinya SAMA dengan dialog yang digulir langsung.
// Pasang SEBELUM verticalScroll / pada pembungkus LazyColumn, dan hanya di penggulir selebar isi dialog.
// ---------------------------------------------------------------------------------------------

/** Untuk Column + verticalScroll(state). Pasang di modifier SEBELUM .verticalScroll(state). */
fun Modifier.gutterScrollbar(state: ScrollState): Modifier = this.drawWithContent {
    drawContent()
    val max = state.maxValue
    if (max > 0 && max != Int.MAX_VALUE) {
        val inset = 6.dp.toPx()
        val trackH = size.height - inset * 2
        val thumbH = thumbHeight(trackH, size.height.toInt(), max, 36.dp.toPx())
        val w = 3.dp.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = 0.38f),
            topLeft = Offset(size.width + (DIALOG_PAD_H.toPx() - w) / 2f, inset + (trackH - thumbH) * (state.value.toFloat() / max)),
            size = Size(w, thumbH),
            cornerRadius = CornerRadius(w / 2f)
        )
    }
}

/** Untuk LazyColumn(state): pasang di Box pembungkus LazyColumn (perkiraan posisi dari indeks item yang tampak). */
fun Modifier.gutterScrollbar(state: LazyListState): Modifier = this.drawWithContent {
    drawContent()
    val info = state.layoutInfo
    val total = info.totalItemsCount
    val visible = info.visibleItemsInfo
    if (total > 0 && visible.isNotEmpty() && (state.canScrollForward || state.canScrollBackward)) {
        val inset = 6.dp.toPx()
        val trackH = size.height - inset * 2
        val first = visible.first()
        val itemH = first.size.coerceAtLeast(1)
        val posItems = state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset.toFloat() / itemH
        val viewItems = (info.viewportEndOffset - info.viewportStartOffset).toFloat() / itemH
        val thumbH = (trackH * (viewItems / total).coerceIn(0f, 1f)).coerceAtLeast(36.dp.toPx()).coerceAtMost(trackH)
        val frac = (posItems / (total - viewItems).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val w = 3.dp.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = 0.38f),
            topLeft = Offset(size.width + (DIALOG_PAD_H.toPx() - w) / 2f, inset + (trackH - thumbH) * frac),
            size = Size(w, thumbH),
            cornerRadius = CornerRadius(w / 2f)
        )
    }
}
