package magi.aenerv7.ppembytv.data.model

enum class VideoResolution(
    val sortValue: Int,
    private val standardLabel: String,
    private val uppercaseLabel: String,
) {
    UHD_4K(2160, "4K", "4K"),
    QHD_2K(1440, "2K", "2K"),
    FULL_HD_1080(1080, "1080p", "1080P"),
    HD_720(720, "720p", "720P"),
    SD_480(480, "480p", "480P");

    fun label(style: ResolutionLabelStyle): String = when (style) {
        ResolutionLabelStyle.STANDARD -> standardLabel
        ResolutionLabelStyle.UPPERCASE -> uppercaseLabel
    }
}
