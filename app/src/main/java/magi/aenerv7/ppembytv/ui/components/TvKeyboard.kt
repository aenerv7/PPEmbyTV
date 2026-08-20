package magi.aenerv7.ppembytv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val rows = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m"),
    listOf(".", ":", "/", "-", "_", "@", "\\"),
)

@Composable
fun TvKeyboard(
    initialValue: String,
    title: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                text = text.ifEmpty { " " },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            )
            Spacer(Modifier.height(16.dp))

            for (row in rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (key in row) {
                        KeyButton(key) { text += key }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KeyButton("空格", wide = true) { text += " " }
                KeyButton("退格", wide = true) { if (text.isNotEmpty()) text = text.dropLast(1) }
                KeyButton("清空", wide = true) { text = "" }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvButton("完成") { onDone(text) }
                TvButton("取消") { onCancel() }
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, wide: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(if (wide) 96.dp else 52.dp)
            .height(44.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}
