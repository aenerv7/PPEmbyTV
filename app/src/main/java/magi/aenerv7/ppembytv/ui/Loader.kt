package magi.aenerv7.ppembytv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * 简单的挂起加载器：key 变化时重新执行 load。
 */
@Composable
fun <T> rememberLoad(key: Any?, load: suspend () -> T): UiState<T> {
    var state by remember(key) { mutableStateOf<UiState<T>>(UiState.Loading) }
    LaunchedEffect(key) {
        state = UiState.Loading
        state = try {
            UiState.Success(load())
        } catch (e: Exception) {
            UiState.Error(e.message ?: "加载失败")
        }
    }
    return state
}
