package magi.aenerv7.ppembytv.data.model

object AppCardSizeLevel {
    const val MIN = 1
    const val DEFAULT = 5
    const val MAX = 12

    fun normalize(level: Int): Int = level.coerceIn(MIN, MAX)

    fun next(level: Int): Int {
        val normalized = normalize(level)
        return if (normalized >= MAX) MIN else normalized + 1
    }
}
