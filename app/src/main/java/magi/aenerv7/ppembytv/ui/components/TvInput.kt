package magi.aenerv7.ppembytv.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import magi.aenerv7.ppembytv.ui.theme.TvFocusBorder
import magi.aenerv7.ppembytv.ui.theme.TvInputContainer

/**
 * 复刻参考 App 的 TvConfirmEditableOutlinedTextField（TvInput.kt）：
 * - 默认只读，聚焦后按 Enter/确认键 进入编辑态，此时弹出**系统输入法（IME）**；
 * - 输入法 Done / Enter / 返回键 / 失焦 时退出编辑态并收起输入法；
 * - 编辑态下按方向键会先退出编辑态再移动焦点；
 * - 聚焦时显示 2dp 白色高亮边框（原版 tvOutlinedTextFieldColors）。
 */
@Composable
fun TvOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    var editing by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val localFocusRequester = remember { FocusRequester() }
    val effectiveFocusRequester = focusRequester ?: localFocusRequester
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    fun exitEditing() {
        if (editing) {
            editing = false
            keyboard?.hide()
        }
    }

    fun enterEditing() {
        if (!editing) {
            editing = true
        }
    }

    LaunchedEffect(pressed) {
        if (pressed && enabled) enterEditing()
    }
    LaunchedEffect(editing) {
        if (editing) {
            effectiveFocusRequester.requestFocus()
            keyboard?.show()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (up != null && enabled) enterEditing()
                }
            }
            .focusRequester(effectiveFocusRequester)
            .onFocusChanged { if (!it.isFocused) exitEditing() }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                            if (editing) exitEditing() else enterEditing()
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                            if (editing) {
                                exitEditing()
                                when (event.key) {
                                    Key.DirectionUp -> focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Up)
                                    Key.DirectionDown -> focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                                    Key.DirectionLeft -> focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Left)
                                    Key.DirectionRight -> focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right)
                                    else -> Unit
                                }
                                true
                            } else {
                                false
                            }
                        }
                        Key.Back, Key.Escape -> {
                            if (editing) {
                                exitEditing()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        enabled = enabled,
        interactionSource = interactionSource,
        readOnly = !editing,
        label = label?.let { { Text(text = it) } },
        placeholder = placeholder?.let { { Text(text = it) } },
        singleLine = singleLine,
        visualTransformation = if (keyboardType == KeyboardType.Password && visualTransformation == VisualTransformation.None) {
            PasswordVisualTransformation()
        } else {
            visualTransformation
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { exitEditing() },
        ),
        colors = tvOutlinedTextFieldColors(),
        shape = RoundedCornerShape(10.dp),
    )
}

/** 复刻参考 App 的 tvOutlinedTextFieldColors（TvInput.kt:41）。 */
@Composable
fun tvOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    errorTextColor = MaterialTheme.colorScheme.error,
    focusedContainerColor = TvInputContainer,
    unfocusedContainerColor = TvInputContainer,
    disabledContainerColor = TvInputContainer.copy(alpha = 0.42f),
    errorContainerColor = TvInputContainer,
    cursorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    focusedBorderColor = TvFocusBorder,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
)
