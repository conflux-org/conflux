package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.conflux_org.conflux.core.ui.theme.AppTextFieldStyle

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = AppTextFieldStyle,
        modifier = modifier,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        placeholder = placeholder,
        label = label,
        shape = RoundedCornerShape(10.dp),
        colors = darkTextFieldColors(),
    )
}

@Composable
fun darkTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color(0xFFB5B5B5),
        focusedBorderColor = Color(0xFF6C6CB5),
        unfocusedBorderColor = Color(0xFF8A8A8A),
        cursorColor = Color.White,
    )
