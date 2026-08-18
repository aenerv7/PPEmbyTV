package magi.aenerv7.ppembytv.ui.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleDecoderException
import androidx.media3.extractor.text.SubtitleInputBuffer
import androidx.media3.extractor.text.SubtitleOutputBuffer

/**
 * 字幕延迟状态（毫秒，正数=字幕延后显示）。
 */
object SubtitleOffset {
    @Volatile
    var offsetMs: Long = 0L
}

/**
 * 通过包装默认 SubtitleDecoderFactory 实现字幕延迟：
 * 解码输出时把 buffer 时间整体平移 offset，TextRenderer 即会按偏移后的时间显示字幕。
 */
class OffsetSubtitleDecoderFactory(
    private val offsetUs: () -> Long,
) : SubtitleDecoderFactory {

    private val delegate: SubtitleDecoderFactory = SubtitleDecoderFactory.DEFAULT

    override fun supportsFormat(format: Format): Boolean = delegate.supportsFormat(format)

    override fun createDecoder(format: Format): SubtitleDecoder {
        val inner = delegate.createDecoder(format)
        return object : SubtitleDecoder {
            override fun getName(): String = inner.name

            override fun setOutputStartTimeUs(timeUs: Long) = inner.setOutputStartTimeUs(timeUs)

            override fun setPositionUs(timeUs: Long) = inner.setPositionUs(timeUs)

            override fun dequeueInputBuffer(): SubtitleInputBuffer? = inner.dequeueInputBuffer()

            override fun queueInputBuffer(inputBuffer: SubtitleInputBuffer) =
                inner.queueInputBuffer(inputBuffer)

            override fun dequeueOutputBuffer(): SubtitleOutputBuffer? {
                val out = inner.dequeueOutputBuffer()
                if (out != null) {
                    val off = offsetUs()
                    if (off != 0L) {
                        out.timeUs += off
                    }
                }
                return out
            }

            override fun flush() = inner.flush()

            override fun release() = inner.release()
        }
    }
}

/**
 * 使用带字幕延迟能力的 TextRenderer 的 RenderersFactory。
 */
class OffsetRenderersFactory(
    context: Context,
    private val offsetUs: () -> Long,
) : DefaultRenderersFactory(context) {

    override fun buildTextRenderers(
        context: Context,
        textOutput: TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: java.util.ArrayList<Renderer>,
    ) {
        out.add(TextRenderer(textOutput, outputLooper, OffsetSubtitleDecoderFactory(offsetUs)))
    }
}
