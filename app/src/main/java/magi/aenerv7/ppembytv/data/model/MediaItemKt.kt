package magi.aenerv7.ppembytv.data.model

fun MediaItem.isLiveTvChannel(): Boolean = type.equals("TvChannel", true)

fun MediaItem.primaryArtworkAspectRatio(): Double? {
    if (imageTags?.primary != null) {
        return primaryImageAspectRatio
    }
    programPrimaryImageAspectRatio?.let { return it }
    currentProgram?.let { return it.primaryImageAspectRatio }
    return null
}
