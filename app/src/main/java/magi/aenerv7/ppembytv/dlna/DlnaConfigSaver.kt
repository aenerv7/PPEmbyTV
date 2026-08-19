package magi.aenerv7.ppembytv.dlna

import android.content.Context

/**
 * Saves the DLNA settings from the UI state and restarts (or stops) the DLNA service,
 * then invokes [onDone].
 *
 * Ported from the obfuscated `a.java` (`com.dh.myembyapp.dlna.a`), which was a
 * `SuspendLambda` (Function2) that:
 *  1. built a [DlnaConfig] from the four UI `MutableState`s — note it hard-codes
 *     `autoPlay = true` exactly like the original,
 *  2. called [DlnaSettings.saveConfig],
 *  3. stopped + restarted [DlnaService] when enabled, or just stopped it when disabled,
 *  4. invoked the completion callback.
 *
 * The two extra UI states map to `useProxyByDefault` and `trustAllCerts`; the original
 * had no auto-play state. File renamed `a.java` → `DlnaConfigSaver.kt`.
 */
suspend fun saveDlnaConfigAndRestartService(
    settings: DlnaSettings,
    context: Context,
    enabled: Boolean,
    deviceName: String,
    useProxyByDefault: Boolean,
    trustAllCerts: Boolean,
    onDone: () -> Unit,
) {
    val name = deviceName.ifBlank { "皮皮 TV" }
    settings.saveConfig(
        DlnaConfig(
            enabled = enabled,
            deviceName = name,
            autoPlay = true,
            useProxyByDefault = useProxyByDefault,
            trustAllCerts = trustAllCerts,
        )
    )
    if (enabled) {
        DlnaService.stop(context)
        DlnaService.start(context)
    } else {
        DlnaService.stop(context)
    }
    onDone()
}
