package magi.aenerv7.ppembytv.util

import magi.aenerv7.ppembytv.api.BaseItemDto
import java.util.Locale

object Formatting {

    /** ticks（100ns）转 秒 */
    fun ticksToSeconds(ticks: Long): Long = ticks / 10_000_000L

    fun secondsToTicks(seconds: Long): Long = seconds * 10_000_000L

    /** 秒转 mm:ss 或 h:mm:ss */
    fun formatDuration(seconds: Long): String {
        if (seconds < 0) return "0:00"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    /** 集/季的辅助标题 */
    fun itemSubtitle(item: BaseItemDto): String? {
        return when {
            item.type == "Episode" -> {
                val ep = item.indexNumber?.let { "S${item.parentIndexNumber ?: 0}E$it" }
                listOfNotNull(ep, item.seriesName).joinToString(" · ").ifEmpty { null }
            }
            item.type == "Movie" -> item.productionYear?.toString()
            item.type == "Series" -> {
                val years = item.productionYear?.toString()
                val seasons = item.seasonCount?.let { "${it}季" }
                listOfNotNull(years, seasons).joinToString(" · ").ifEmpty { null }
            }
            else -> item.productionYear?.toString()
        }
    }

    /** 评分显示 */
    fun rating(communityRating: Double?): String? =
        communityRating?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1f", it) }

    fun runtime(runTimeTicks: Long?): String? =
        runTimeTicks?.takeIf { it > 0 }?.let { "${ticksToSeconds(it) / 60} 分钟" }
}
