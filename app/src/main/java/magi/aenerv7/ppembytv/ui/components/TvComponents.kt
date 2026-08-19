package magi.aenerv7.ppembytv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import magi.aenerv7.ppembytv.data.api.RetrofitClient

private val FocusBorderColor = Color(0xFF4C8DFF)

/** Makes a composable focusable with D-pad navigation and an Enter/Center click trigger. */
fun Modifier.tvClickable(
    onClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
): Modifier = this
    .focusable()
    .onFocusChanged { onFocusChanged(it.isFocused) }
    .onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp &&
            (event.key == Key.Enter || event.key == Key.DirectionCenter || event.key == Key.NumPadEnter)
        ) {
            onClick()
            true
        } else {
            false
        }
    }

@Composable
fun TvButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "btnScale")
    Row(
        modifier = modifier
            .scale(scale)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) FocusBorderColor else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Int = 160,
    height: Int = 240,
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "posterScale")
    Column(
        modifier = modifier
            .width(width.dp)
            .scale(scale)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .border(
                width = 3.dp,
                color = if (focused) FocusBorderColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(width.dp)
                .height(height.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

fun imageUrl(itemId: String?, imageType: String = "Primary", tag: String?, maxWidth: Int = 320): String? {
    if (itemId == null) return null
    return RetrofitClient.getImageUrl(itemId, imageType, tag, maxWidth)
}

fun backdropUrl(itemId: String?, tag: String?, maxWidth: Int = 1920): String? {
    if (itemId == null) return null
    return RetrofitClient.getImageUrl(itemId, "Backdrop", tag, maxWidth)
}
