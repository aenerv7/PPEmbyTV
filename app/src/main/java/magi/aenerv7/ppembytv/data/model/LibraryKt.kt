package magi.aenerv7.ppembytv.data.model

fun String?.isLiveTvCollectionType(): Boolean =
    equals("livetv", true) || equals("channels", true)

fun Library.isLiveTvLibrary(): Boolean = collectionType.isLiveTvCollectionType()
