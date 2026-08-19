package magi.aenerv7.ppembytv.data.model

enum class SubtitleFormatPriorityType(
    val storageValue: String,
    val label: String,
    val defaultPriority: Int = 1,
) {
    ASS_SSA("ass_ssa", "ASS/SSA"),
    SRT("srt", "SRT"),
    VTT("vtt", "VTT"),
    PGS("pgs", "PGS"),
    DVB("dvb", "DVB"),
    SUB("sub", "SUB"),
    OTHER("other", "其他");

    companion object {
        fun fromStorageValue(value: String?): SubtitleFormatPriorityType? =
            entries.firstOrNull { it.storageValue == value }

        /**
         * Maps a (case-insensitive, trimmed) format label to a known type, defaulting
         * to [OTHER]. The decompiled switch was only partially restored; the label
         * groups below follow the surviving bytecode fragments.
         */
        fun fromLabel(label: String?): SubtitleFormatPriorityType {
            val upper = label?.trim()?.uppercase(java.util.Locale.ROOT) ?: return OTHER
            return when (upper) {
                "ASS/SSA", "ASS", "SSA" -> ASS_SSA
                "SRT", "SUBRIP" -> SRT
                "VTT", "WEBVTT" -> VTT
                "PGS", "PGSSUB", "HDMV_PGS_SUBTITLE" -> PGS
                "DVB", "DVBSUB" -> DVB
                "SUB" -> SUB
                else -> OTHER
            }
        }
    }
}
