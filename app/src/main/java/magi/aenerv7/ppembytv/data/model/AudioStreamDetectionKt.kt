package magi.aenerv7.ppembytv.data.model

import java.util.Locale

/**
 * Heuristics for detecting the codec/language of an audio stream from its
 * metadata (MIME type, codecs string, titles, language codes).
 */

private const val LANGUAGE_UNDETERMINED = "und"

/** True when [text] mentions any of the [keywords] (case-insensitive). */
private fun containsAudioLanguageKeyword(text: String, vararg keywords: String): Boolean {
    for (keyword in keywords) {
        val lower = keyword.lowercase(Locale.ROOT)
        val found: Boolean = if (lower.any { it in 'a'..'z' }) {
            Regex("(?<![a-z])" + Regex.escape(lower) + "(?![a-z])").containsMatchIn(text)
        } else {
            text.contains(lower)
        }
        if (found) return true
    }
    return false
}

fun detectAudioLanguagePreferenceFromCode(language: String?): AudioLanguagePreference? {
    val normalized = normalizeAudioLanguage(language) ?: return null
    return when (normalized) {
        "de" -> AudioLanguagePreference.GERMAN
        "en" -> AudioLanguagePreference.ENGLISH
        "es" -> AudioLanguagePreference.SPANISH
        "fr" -> AudioLanguagePreference.FRENCH
        "it" -> AudioLanguagePreference.ITALIAN
        "ja" -> AudioLanguagePreference.JAPANESE
        "ko" -> AudioLanguagePreference.KOREAN
        "ru" -> AudioLanguagePreference.RUSSIAN
        "th" -> AudioLanguagePreference.THAI
        "zh" -> AudioLanguagePreference.CHINESE
        "yue" -> AudioLanguagePreference.CANTONESE
        else -> null
    }
}

fun detectAudioLanguagePreferenceFromText(text: String?): AudioLanguagePreference? {
    val lower = text?.lowercase(Locale.ROOT) ?: ""
    if (lower.isBlank()) return null
    if (containsAudioLanguageKeyword(lower, "粤语", "廣東話", "广东话", "廣東語", "cantonese")) {
        return AudioLanguagePreference.CANTONESE
    }
    if (containsAudioLanguageKeyword(lower, "中文", "国语", "國語", "普通话", "普通話", "华语", "華語", "mandarin", "chinese")) {
        return AudioLanguagePreference.CHINESE
    }
    if (containsAudioLanguageKeyword(lower, "英语", "英文", "english")) {
        return AudioLanguagePreference.ENGLISH
    }
    if (containsAudioLanguageKeyword(lower, "韩语", "韓語", "朝鲜语", "朝鮮語", "korean")) {
        return AudioLanguagePreference.KOREAN
    }
    if (containsAudioLanguageKeyword(lower, "日语", "日語", "japanese")) {
        return AudioLanguagePreference.JAPANESE
    }
    if (containsAudioLanguageKeyword(lower, "法语", "法語", "french")) {
        return AudioLanguagePreference.FRENCH
    }
    if (containsAudioLanguageKeyword(lower, "德语", "德語", "german")) {
        return AudioLanguagePreference.GERMAN
    }
    if (containsAudioLanguageKeyword(lower, "西班牙语", "西班牙語", "spanish")) {
        return AudioLanguagePreference.SPANISH
    }
    if (containsAudioLanguageKeyword(lower, "俄语", "俄語", "russian")) {
        return AudioLanguagePreference.RUSSIAN
    }
    if (containsAudioLanguageKeyword(lower, "意大利语", "意大利語", "italian")) {
        return AudioLanguagePreference.ITALIAN
    }
    if (containsAudioLanguageKeyword(lower, "泰语", "泰語", "thai")) {
        return AudioLanguagePreference.THAI
    }
    return null
}

/**
 * Canonicalizes a language code: trims/lowercases it, maps known variants
 * (e.g. "zh-hant", "chi", "zho" -> "zh"; "yue-hk", "zh-yue" -> "yue").
 * Returns the lowercased input when unknown, or null for blank input.
 */
fun normalizeAudioLanguage(language: String?): String? {
    val normalized = language?.trim()?.lowercase(Locale.ROOT) ?: ""
    return when (normalized) {
        "" -> null
        "yue", "yue-cn", "yue-hk", "yue-hans", "yue-hant", "zh-yue" -> "yue"
        "zh", "zh-hans", "zh-hant", "zh-cn", "zh-hk", "zh-mo", "zh-tw", "chi", "zho" -> "zh"
        "de", "deu", "ger" -> "de"
        "en", "eng" -> "en"
        "es", "spa" -> "es"
        "fr", "fra", "fre" -> "fr"
        "it", "ita" -> "it"
        "ja", "jpn" -> "ja"
        "ko", "kor" -> "ko"
        "ru", "rus" -> "ru"
        "th", "tha" -> "th"
        LANGUAGE_UNDETERMINED -> LANGUAGE_UNDETERMINED
        else -> normalized
    }
}

/** Detects the audio codec family from a single media stream. */
fun resolveAudioCodecPriorityType(stream: MediaStream): AudioCodecPriorityType {
    val text = listOfNotNull(stream.codec, stream.displayTitle, stream.title)
        .joinToString(" ")
        .lowercase(Locale.ROOT)
    return resolveAudioCodecPriorityTypeFromText(text)
}

/** Detects the audio codec family from MIME type / codecs / display labels. */
fun resolveAudioCodecPriorityType(
    sampleMimeType: String?,
    codecs: String?,
    titleLabel: String?,
    formatLabel: String?,
): AudioCodecPriorityType {
    val text = listOfNotNull(sampleMimeType, codecs, titleLabel, formatLabel)
        .joinToString(" ")
        .lowercase(Locale.ROOT)
    return resolveAudioCodecPriorityTypeFromText(text)
}

private fun resolveAudioCodecPriorityTypeFromText(text: String): AudioCodecPriorityType {
    val normalized = text.replace("-", "").replace("_", "").replace(" ", "")
    return when {
        normalized.contains("truehd") || normalized.contains("mlp") -> AudioCodecPriorityType.TRUEHD
        normalized.contains("eac3") || normalized.contains("ec3") -> AudioCodecPriorityType.EAC3
        Regex("(?<!e)ac-?3").containsMatchIn(text) || normalized.contains("ac3") -> AudioCodecPriorityType.AC3
        normalized.contains("dts") -> AudioCodecPriorityType.DTS
        normalized.contains("flac") -> AudioCodecPriorityType.FLAC
        normalized.contains("aac") || normalized.contains("mp4a") -> AudioCodecPriorityType.AAC
        else -> AudioCodecPriorityType.OTHER
    }
}

/** Detects the preferred language from descriptive text, falling back to the language codes. */
fun resolveAudioLanguagePreference(descriptiveText: String?, vararg languageCodes: String?): AudioLanguagePreference? {
    detectAudioLanguagePreferenceFromText(descriptiveText)?.let { return it }
    for (code in languageCodes) {
        detectAudioLanguagePreferenceFromCode(code)?.let { return it }
    }
    return null
}
