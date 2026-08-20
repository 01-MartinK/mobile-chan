package com.mk.mobilechan.ui.threads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import androidx.core.text.HtmlCompat
import com.mk.mobilechan.R
import com.mk.mobilechan.data.ChanHttp
import com.mk.mobilechan.data.ChanMedia
import com.mk.mobilechan.data.IndexThread
import com.mk.mobilechan.data.Post
import com.mk.mobilechan.ui.theme.MobileChanTheme

@Composable
fun ThreadCard(
    thread: IndexThread,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onQuoteClick: ((Long) -> Unit)? = null,
    highlighted: Boolean = false,
    showImages: Boolean = true,
    showVideos: Boolean = true,
    isBookmarked: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
) {
    val op = thread.op ?: return
    var showFullRes by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val isVideo = ChanMedia.isVideo(op.ext, op.mime)
    val showMedia = if (isVideo) showVideos else showImages

    val colors = CardDefaults.cardColors(
        containerColor = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    val cardModifier = modifier.fillMaxWidth()
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThreadHeader(op)
            if (showMedia) {
                ThreadImage(
                    thumbnailUrl = thread.thumbnailUrl,
                    fullImageUrl = thread.imageUrl,
                    filename = op.filename,
                    onClick = { showFullRes = true },
                    onLongClick = { menuExpanded = true },
                )
            }
            op.sub?.takeIf { it.isNotBlank() }?.let { subject ->
                val unescapedSubject = remember(subject) { unescapeHtml(subject) }
                Text(
                    text = unescapedSubject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            op.com?.takeIf { it.isNotBlank() }?.let { comment ->
                ChanComment(html = comment, onQuoteClick = onQuoteClick)
            }
            ThreadMeta(op)
        }
    }

    Box(modifier = cardModifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = { menuExpanded = true },
                    onLongClickLabel = stringResource(R.string.thread_menu),
                ),
            colors = colors,
            elevation = elevation,
            content = { content() },
        )
        ThreadContextMenu(
            thread = thread,
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            isBookmarked = isBookmarked,
            onToggleBookmark = onToggleBookmark,
        )
    }

    if (showFullRes && showMedia) {
        thread.imageUrl?.let { url ->
            FullMediaOverlay(
                url = url,
                ext = op.ext,
                mime = op.mime,
                filename = op.filename,
                onDismiss = { showFullRes = false },
            )
        }
    }
}

@Composable
private fun ThreadHeader(op: Post) {
    val name = buildString {
        append(op.name)
        op.trip?.let { append(it) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = op.now,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.thread_no, op.no),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThreadImage(
    thumbnailUrl: String?,
    fullImageUrl: String?,
    filename: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    if (thumbnailUrl == null) return
    AsyncImage(
        model = thumbnailUrl,
        contentDescription = filename ?: stringResource(R.string.thread_image),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { if (fullImageUrl != null) onClick() },
                onLongClick = onLongClick,
                onLongClickLabel = stringResource(R.string.thread_menu),
            ),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun FullMediaOverlay(
    url: String,
    ext: String?,
    mime: String? = null,
    filename: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            if (ChanMedia.isVideo(ext, mime)) {
                FullVideoPlayer(
                    url = url,
                    filename = filename,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            } else {
                ZoomableFullImage(
                    url = url,
                    filename = filename,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun ZoomableFullImage(
    url: String,
    filename: String?,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(MIN_IMAGE_SCALE, MAX_IMAGE_SCALE)
        scale = nextScale
        offset = if (nextScale > 1f) offset + panChange else Offset.Zero
    }

    AsyncImage(
        model = url,
        contentDescription = filename ?: stringResource(R.string.thread_image),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .transformable(
                state = transformState,
                lockRotationOnZoomPan = true,
                canPan = { scale > 1f },
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            onDismiss()
                        }
                    },
                )
            },
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun FullVideoPlayer(
    url: String,
    filename: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    DefaultHttpDataSource.Factory().setUserAgent(ChanHttp.USER_AGENT),
                ),
            )
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                prepare()
            }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                contentDescription = filename ?: viewContext.getString(R.string.thread_video)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ThreadMeta(op: Post) {
    val flags = buildList {
        if (op.sticky == 1) add(stringResource(R.string.thread_sticky))
        if (op.closed == 1) add(stringResource(R.string.thread_closed))
    }
    if (op.filename != null && op.ext != null) {
        Text(
            text = "${op.filename}${op.ext}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (flags.isNotEmpty()) {
        Text(
            text = flags.joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (op.replies > 0 || op.images > 0) {
        Text(
            text = stringResource(R.string.thread_stats, op.replies, op.images),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (op.omitted_posts > 0) {
        Text(
            text = stringResource(R.string.thread_omitted, op.omitted_posts),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChanComment(
    html: String,
    onQuoteClick: ((Long) -> Unit)? = null,
) {
    val blocks = remember(html) { parseChanComment(html) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is CommentBlock.Text -> Text(
                    text = block.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is CommentBlock.QuoteLink -> Text(
                    text = block.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = if (onQuoteClick != null && block.postNo != null) {
                        Modifier.clickable { onQuoteClick(block.postNo) }
                    } else {
                        Modifier
                    },
                )
                is CommentBlock.Quote -> QuoteBlock(block.value)
            }
        }
    }
}

@Composable
private fun QuoteBlock(text: String) {
    val quoteColor = MaterialTheme.colorScheme.primary
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = quoteColor,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = quoteColor,
                    size = Size(3.dp.toPx(), size.height),
                )
            }
            .padding(start = 10.dp),
    )
}

private sealed interface CommentBlock {
    data class Text(val value: String) : CommentBlock
    data class QuoteLink(val value: String, val postNo: Long?) : CommentBlock
    data class Quote(val value: String) : CommentBlock
}

private fun parseChanComment(html: String): List<CommentBlock> {
    val normalized = html
        .replace(BR_TAG, "\n")
        .replace(WBR_TAG, "")
    val blocks = mutableListOf<CommentBlock>()
    var last = 0

    fun addText(raw: String) {
        val text = unescapeHtml(raw).trim()
        if (text.isNotEmpty()) blocks += CommentBlock.Text(text)
    }

    for (match in COMMENT_TOKEN.findAll(normalized)) {
        addText(normalized.substring(last, match.range.first))
        val quote = match.groups[1]?.value
        val link = match.groups[2]?.value
        when {
            quote != null -> {
                val text = unescapeHtml(STRIP_TAGS.replace(quote, ""))
                    .trim()
                    .removePrefix(">")
                    .trim()
                if (text.isNotEmpty()) blocks += CommentBlock.Quote(text)
            }
            link != null -> {
                val text = unescapeHtml(STRIP_TAGS.replace(link, "")).trim()
                if (text.isNotEmpty()) {
                    val postNo = quotePostNo(match.value, text)
                    blocks += CommentBlock.QuoteLink(text, postNo)
                }
            }
        }
        last = match.range.last + 1
    }
    addText(normalized.substring(last))
    return blocks
}

private fun unescapeHtml(value: String): String {
    val protected = value.replace("\n", NEWLINE_PLACEHOLDER)
    return HtmlCompat.fromHtml(protected, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(NEWLINE_PLACEHOLDER, "\n")
}

private fun quotePostNo(rawTag: String, text: String): Long? =
    HREF_POST_NO.find(rawTag)?.groupValues?.get(1)?.toLongOrNull()
        ?: QUOTE_LINK_NO.find(text)?.groupValues?.get(1)?.toLongOrNull()

private const val MIN_IMAGE_SCALE = 1f
private const val MAX_IMAGE_SCALE = 5f
private const val NEWLINE_PLACEHOLDER = "\u0000"
private val BR_TAG = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val WBR_TAG = Regex("<wbr\\s*/?>", RegexOption.IGNORE_CASE)
private val STRIP_TAGS = Regex("<[^>]+>")
private val HREF_POST_NO = Regex("""#p?(\d+)""", RegexOption.IGNORE_CASE)
private val QUOTE_LINK_NO = Regex(""">>(\d+)""")
private val COMMENT_TOKEN = Regex(
    """<span class="quote">([\s\S]*?)</span>|<a[^>]*class="quotelink"[^>]*>([\s\S]*?)</a>|<[^>]+>""",
    RegexOption.IGNORE_CASE,
)

@Preview(showBackground = true)
@Composable
private fun ThreadCardPreview() {
    MobileChanTheme {
        ThreadCard(
            thread = IndexThread(
                posts = listOf(
                    Post(
                        no = 9823415,
                        now = "05/14/24(Tue)14:28:05",
                        name = "Anonymous",
                        com = """<a href="#p9823401" class="quotelink">&gt;&gt;9823401</a><br><span class="quote">&gt;I want to keep the physical footprint as small as possible while maximizing throughput.</span><br>N100s are fine for basic stuff, but if you want real throughput between nodes you need 2.5GbE minimum, preferably 10GbE if you are moving big files around. Most cheap mini PCs only have gigabit.<br>Look into the Lenovo Tiny series or HP Mini's off eBay. You can usually slot a 10G NIC in the PCIe slot if you get the right model. Costs a bit more upfront but worth it for the expandability.""",
                        replies = 42,
                        images = 8,
                    ),
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
