package com.tietiezhi.terminal.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tietiezhi.terminal.emulator.Cell
import com.tietiezhi.terminal.emulator.TextStyle
import com.tietiezhi.terminal.session.TerminalSession

/**
 * Terminal View Compose component
 */
@Composable
fun TerminalView(
    session: TerminalSession,
    modifier: Modifier = Modifier,
    fontSize: Float = 14f,
    backgroundColor: Color = Color(0xFF1E1E1E),
    foregroundColor: Color = Color(0xFFD4D4D4),
    showCursor: Boolean = true,
    onTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val density = LocalDensity.current
    val lineHeight = with(density) { (fontSize * 1.2).sp.toDp() }
    val charWidth = with(density) { (fontSize * 0.6).sp.toDp() }
    
    val horizontalScrollState = rememberScrollState()
    
    val screenFlow = session.emulator.screenUpdates
    val cursorFlow = session.emulator.cursorPosition
    
    var screenLines by remember { mutableStateOf(emptyList<List<Cell>>()) }
    var cursorPos by remember { mutableStateOf(Pair(0, 0)) }
    
    LaunchedEffect(screenFlow) {
        screenFlow.collect { lines ->
            screenLines = lines
        }
    }
    
    LaunchedEffect(cursorFlow) {
        cursorFlow.collect { pos ->
            cursorPos = pos
        }
    }
    
    Box(
        modifier = modifier
            .background(backgroundColor)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Terminal content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    screenLines.forEachIndexed { rowIndex, line ->
                        Row(
                            modifier = Modifier.height(lineHeight),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            line.forEachIndexed { colIndex, cell ->
                                val isCursor = rowIndex == cursorPos.first && colIndex == cursorPos.second
                                TerminalCell(
                                    cell = cell,
                                    fontSize = fontSize,
                                    foregroundColor = foregroundColor,
                                    showCursor = showCursor && isCursor,
                                    cursorColor = foregroundColor
                                )
                            }
                            // Ensure row spans full width
                            Spacer(modifier = Modifier.width(100.dp))
                        }
                    }
                    // Ensure minimum height
                    if (screenLines.isEmpty()) {
                        Spacer(modifier = Modifier.height(lineHeight * 24))
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalCell(
    cell: Cell,
    fontSize: Float,
    foregroundColor: Color,
    showCursor: Boolean,
    cursorColor: Color
) {
    val foreColor = if (cell.foreColor in TextStyle.ANSI_COLORS.indices) {
        TextStyle.ANSI_COLORS[cell.foreColor]
    } else {
        foregroundColor
    }
    
    val backColor = if (cell.backColor in TextStyle.ANSI_COLORS.indices) {
        TextStyle.ANSI_COLORS[cell.backColor]
    } else {
        Color.Transparent
    }
    
    val displayChar = if (cell.concealed) ' ' else cell.char
    val effectiveForeColor = if (cell.inverse) backColor else foreColor
    val effectiveBackColor = if (cell.inverse) foreColor else backColor
    
    val textDecoration = when {
        cell.underline -> TextDecoration.Underline
        cell.strikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }
    
    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        color = effectiveForeColor,
        textDecoration = textDecoration
    )
    
    Box(
        modifier = Modifier
            .width(with(LocalDensity.current) { (fontSize * 0.6).sp.toDp() })
            .height(with(LocalDensity.current) { (fontSize * 1.2).sp.toDp() })
            .background(if (effectiveBackColor != Color.Transparent) effectiveBackColor else Color.Transparent)
            .drawBehind {
                if (showCursor && cell.cursor) {
                    drawRect(cursorColor.copy(alpha = 0.5f))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = displayChar.toString(),
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

/**
 * Terminal keyboard component for special keys
 */
@Composable
fun TerminalKeyboard(
    onKeyEvent: (TerminalKeyEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D))
            .padding(4.dp)
    ) {
        // Top row - Function keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Ctrl", "Alt", "Tab", "Esc").forEach { key ->
                TerminalKey(
                    label = key,
                    onClick = { onKeyEvent(TerminalKeyEvent.SpecialKey(key)) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TerminalKey(
                label = "▲",
                onClick = { onKeyEvent(TerminalKeyEvent.CursorUp) }
            )
            TerminalKey(
                label = "▼",
                onClick = { onKeyEvent(TerminalKeyEvent.CursorDown) }
            )
            TerminalKey(
                label = "◀",
                onClick = { onKeyEvent(TerminalKeyEvent.CursorLeft) }
            )
            TerminalKey(
                label = "▶",
                onClick = { onKeyEvent(TerminalKeyEvent.CursorRight) }
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Input field
        var inputText by remember { mutableStateOf("") }
        BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f))
                .padding(8.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = Color.White
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii
            ),
            singleLine = true,
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                if (inputText.isEmpty()) {
                    Text(
                        "Type command...",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun TerminalKey(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4A4A4A)
        ),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

sealed class TerminalKeyEvent {
    data class SpecialKey(val key: String) : TerminalKeyEvent()
    data class Text(val text: String) : TerminalKeyEvent()
    data object CursorUp : TerminalKeyEvent()
    data object CursorDown : TerminalKeyEvent()
    data object CursorLeft : TerminalKeyEvent()
    data object CursorRight : TerminalKeyEvent()
    data object Enter : TerminalKeyEvent()
    data object Backspace : TerminalKeyEvent()
}
