package com.tietiezhi.terminal.emulator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Terminal emulator that parses ANSI escape sequences and manages terminal state
 * Based on Termux's terminal-emulator implementation
 */
class TerminalEmulator(
    private val mDefaultForeground: Int = TextStyle.COLOR_INDEX_WHITE,
    private val mDefaultBackground: Int = TextStyle.COLOR_INDEX_BLACK
) {
    companion object {
        const val KEY_CODE_ESCAPE = 0x1B
        const val KEY_CODE_TAB = 0x09
        const val KEY_CODE_ENTER = 0x0D
        const val KEY_CODE_BACKSPACE = 0x7F
        
        // Cursor movement
        const val KEY_CODE_ARROW_UP = 0x80
        const val KEY_CODE_ARROW_DOWN = 0x81
        const val KEY_CODE_ARROW_LEFT = 0x82
        const val KEY_CODE_ARROW_RIGHT = 0x83
        
        // Function keys
        const val KEY_CODE_F1 = 0x90
        const val KEY_CODE_F2 = 0x91
        const val KEY_CODE_F3 = 0x92
        const val KEY_CODE_F4 = 0x93
        const val KEY_CODE_F5 = 0x94
        const val KEY_CODE_F6 = 0x95
        const val KEY_CODE_F7 = 0x96
        const val KEY_CODE_F8 = 0x97
        const val KEY_CODE_F9 = 0x98
        const val KEY_CODE_F10 = 0x99
        const val KEY_CODE_F11 = 0x9A
        const val KEY_CODE_F12 = 0x9B
        
        // Other special keys
        const val KEY_CODE_INSERT = 0xA0
        const val KEY_CODE_DELETE = 0xA1
        const val KEY_CODE_HOME = 0xA2
        const val KEY_CODE_END = 0xA3
        const val KEY_CODE_PAGE_UP = 0xA4
        const val KEY_CODE_PAGE_DOWN = 0xA5
        
        // Special modifiers
        const val KEY_MODIFIER_CTRL = 0x1000
        const val KEY_MODIFIER_ALT = 0x2000
        const val KEY_MODIFIER_SHIFT = 0x4000
    }

    private var mColumns: Int = 80
    private var mRows: Int = 24
    
    private var mBuffer: TerminalBuffer = TerminalBuffer(mColumns, mRows)
    
    private val _screenUpdates = MutableStateFlow<List<List<Cell>>>(emptyList())
    val screenUpdates: StateFlow<List<List<Cell>>> = _screenUpdates.asStateFlow()
    
    private val _cursorPosition = MutableStateFlow(Pair(0, 0))
    val cursorPosition: StateFlow<Pair<Int, Int>> = _cursorPosition.asStateFlow()
    
    private var mTotalScroll: Int = 0
    private var mScrollCounter: Int = 0
    
    // Parse state
    private var mParseBuffer = StringBuilder()
    private var mCurrentScreen: Array<Array<Cell>> = Array(mRows) { Array(mColumns) { Cell() } }
    
    init {
        reset()
    }

    fun reset() {
        mBuffer = TerminalBuffer(mColumns, mRows)
        mTotalScroll = 0
        mScrollCounter = 0
        mParseBuffer.clear()
        updateCurrentScreen()
    }

    fun resize(columns: Int, rows: Int) {
        if (columns == mColumns && rows == mRows) return
        
        val oldBuffer = mBuffer
        mColumns = columns
        mRows = rows
        mBuffer = TerminalBuffer(columns, rows)
        
        // Copy old content
        val minRows = minOf(oldBuffer.rows, rows)
        val minCols = minOf(oldBuffer.columns, columns)
        for (row in 0 until minRows) {
            for (col in 0 until minCols) {
                mBuffer.getCell(row, col).copyFrom(oldBuffer.getCell(row, col))
            }
        }
        
        // Preserve cursor
        mBuffer.setCursor(
            oldBuffer.cursorRow.coerceIn(0, rows - 1),
            oldBuffer.cursorCol.coerceIn(0, columns - 1)
        )
        
        updateCurrentScreen()
    }

    fun getColumns(): Int = mColumns
    fun getRows(): Int = mRows

    fun write(byteBuffer: ByteArray, offset: Int, length: Int) {
        for (i in offset until offset + length) {
            appendToBuffer(byteBuffer[i].toInt() and 0xFF)
        }
        updateCurrentScreen()
    }

    fun write(text: String) {
        for (char in text) {
            appendToBuffer(char.code)
        }
        updateCurrentScreen()
    }

    private fun appendToBuffer(c: Int) {
        when {
            c == 0x1B -> { // ESC
                mParseBuffer.clear()
                mParseBuffer.appendCodePoint(c)
            }
            mParseBuffer.isNotEmpty() -> {
                mParseBuffer.appendCodePoint(c)
                if (processCsiSeq() || processEscSeq() || processOscSeq()) {
                    mParseBuffer.clear()
                }
            }
            c == 0x0D || c == 0x0A || c == 0x07 || c == 0x08 || c == 0x09 || c == 0x7F -> {
                processSpecialChar(c)
            }
            c in 0x20..0x7E || c > 0x7F -> {
                processChar(c)
            }
        }
    }

    private fun processSpecialChar(c: Int) {
        when (c) {
            0x0D -> { // CR
                mBuffer.setCursor(mBuffer.cursorRow, 0)
            }
            0x0A -> { // LF
                if (mBuffer.cursorRow < mBuffer.scrollRegionBottom) {
                    mBuffer.setCursor(mBuffer.cursorRow + 1, mBuffer.cursorCol)
                } else {
                    mBuffer.scrollUp()
                    mTotalScroll++
                }
            }
            0x07 -> { // BEL
                // Could trigger vibration/notification
            }
            0x08 -> { // BS
                if (mBuffer.cursorCol > 0) {
                    mBuffer.setCursor(mBuffer.cursorRow, mBuffer.cursorCol - 1)
                }
            }
            0x09 -> { // TAB
                val tabStop = ((mBuffer.cursorCol / 8) + 1) * 8
                mBuffer.setCursor(mBuffer.cursorRow, minOf(tabStop, mColumns - 1))
            }
            0x7F -> { // DEL
                // Ignore DEL
            }
        }
    }

    private fun processChar(c: Int) {
        val row = mBuffer.cursorRow
        val col = mBuffer.cursorCol
        
        if (col >= mColumns) {
            mBuffer.setCursor(row, 0)
            if (row < mBuffer.scrollRegionBottom) {
                mBuffer.setCursor(row + 1, 0)
            } else {
                mBuffer.scrollUp()
                mTotalScroll++
            }
        }
        
        val cell = mBuffer.getCell(mBuffer.cursorRow, mBuffer.cursorCol)
        cell.char = c.toChar()
        mBuffer.getCurrentStyle().applyTo(cell, mDefaultForeground, mDefaultBackground)
        
        mBuffer.setCursor(mBuffer.cursorRow, mBuffer.cursorCol + 1)
    }

    private fun processEscSeq(): Boolean {
        if (mParseBuffer.length < 2) return false
        
        val secondChar = mParseBuffer[1]
        return when (secondChar) {
            '7' -> { // Save cursor position (SCOSC)
                // Save cursor position
                true
            }
            '8' -> { // Restore cursor position (SCORC)
                // Restore cursor position
                true
            }
            'D' -> { // Index (IND)
                if (mBuffer.cursorRow < mBuffer.scrollRegionBottom) {
                    mBuffer.setCursor(mBuffer.cursorRow + 1, mBuffer.cursorCol)
                } else {
                    mBuffer.scrollUp()
                    mTotalScroll++
                }
                true
            }
            'M' -> { // Reverse Index (RI)
                if (mBuffer.cursorRow > mBuffer.scrollRegionTop) {
                    mBuffer.setCursor(mBuffer.cursorRow - 1, mBuffer.cursorCol)
                } else {
                    mBuffer.scrollDown()
                }
                true
            }
            'E' -> { // Next Line (NEL)
                mBuffer.setCursor(mBuffer.cursorRow, 0)
                if (mBuffer.cursorRow < mBuffer.scrollRegionBottom) {
                    mBuffer.setCursor(mBuffer.cursorRow + 1, 0)
                } else {
                    mBuffer.scrollUp()
                    mTotalScroll++
                }
                true
            }
            'H' -> { // Tab Set (HTS)
                true
            }
            'c' -> { // Full Reset (RIS)
                reset()
                true
            }
            '=', '>' -> { // Keypad Application/DEC mode
                true
            }
            else -> false
        }
    }

    private fun processCsiSeq(): Boolean {
        if (mParseBuffer.length < 3) return false
        if (mParseBuffer[1] != '[') return false
        
        val lastChar = mParseBuffer[mParseBuffer.length - 1]
        if (lastChar.isLetter() || lastChar == '@' || lastChar == '`') {
            return processCsiParameters(lastChar)
        }
        return false
    }

    private fun processCsiParameters(lastChar: Char): Boolean {
        val params = mParseBuffer.substring(2, mParseBuffer.length - 1)
        val style = mBuffer.getCurrentStyle()
        
        when (lastChar) {
            // Cursor movement
            'A' -> { // CUU - Cursor Up
                val count = parseParameter(params, 1)
                mBuffer.moveCursor(-count, 0)
            }
            'B' -> { // CUD - Cursor Down
                val count = parseParameter(params, 1)
                mBuffer.moveCursor(count, 0)
            }
            'C' -> { // CUF - Cursor Forward
                val count = parseParameter(params, 1)
                mBuffer.moveCursor(0, count)
            }
            'D' -> { // CUB - Cursor Back
                val count = parseParameter(params, 1)
                mBuffer.moveCursor(0, -count)
            }
            'E' -> { // CNL - Cursor Next Line
                val count = parseParameter(params, 1)
                mBuffer.setCursor(mBuffer.cursorRow + count, 0)
            }
            'F' -> { // CPL - Cursor Previous Line
                val count = parseParameter(params, 1)
                mBuffer.setCursor(mBuffer.cursorRow - count, 0)
            }
            'G' -> { // CHA - Cursor Horizontal Absolute
                val col = parseParameter(params, 1) - 1
                mBuffer.setCursor(mBuffer.cursorRow, col)
            }
            'H', 'f' -> { // CUP/HVP - Cursor Position
                val parts = params.split(';')
                val row = parseParameter(if (parts.isNotEmpty()) parts[0] else "", 1) - 1
                val col = parseParameter(if (parts.size > 1) parts[1] else "", 1) - 1
                mBuffer.setCursor(row, col)
            }
            'J' -> { // ED - Erase Display
                mBuffer.eraseDisplay(parseParameter(params, 0))
            }
            'K' -> { // EL - Erase Line
                mBuffer.eraseLine(parseParameter(params, 0))
            }
            'L' -> { // IL - Insert Line
                mBuffer.insertLines(parseParameter(params, 1))
            }
            'M' -> { // DL - Delete Line
                mBuffer.deleteLines(parseParameter(params, 1))
            }
            'P' -> { // DCH - Delete Character
                mBuffer.deleteChars(parseParameter(params, 1))
            }
            'S' -> { // SU - Scroll Up
                val count = parseParameter(params, 1)
                repeat(count) {
                    mBuffer.scrollUp()
                    mTotalScroll++
                }
            }
            'T' -> { // SD - Scroll Down
                val count = parseParameter(params, 1)
                repeat(count) {
                    mBuffer.scrollDown()
                }
            }
            'X' -> { // ECH - Erase Character
                val count = parseParameter(params, 1)
                mBuffer.eraseChars(mBuffer.cursorCol, mBuffer.cursorCol + count - 1, mBuffer.cursorRow)
            }
            '@' -> { // ICH - Insert Character
                mBuffer.insertChars(parseParameter(params, 1))
            }
            '`' -> { // HPA - Horizontal Position Absolute
                val col = parseParameter(params, 1) - 1
                mBuffer.setCursor(mBuffer.cursorRow, col)
            }
            
            // SGR - Set Graphics Rendition
            'm' -> {
                val paramList = if (params.isEmpty()) listOf(0) else params.split(';').map { parseParameter(it, 0) }
                for (param in paramList) {
                    when (param) {
                        0 -> style.reset()
                        1 -> style.bold = true
                        3 -> style.italic = true
                        4 -> style.underline = true
                        5, 6 -> style.blinking = true
                        7 -> style.inverse = true
                        9 -> style.strikethrough = true
                        8 -> style.concealed = true
                        22 -> style.bold = false
                        23 -> style.italic = false
                        24 -> style.underline = false
                        25 -> style.blinking = false
                        27 -> style.inverse = false
                        29 -> style.strikethrough = false
                        30, 31, 32, 33, 34, 35, 36, 37 -> style.foreColor = param - 30
                        39 -> style.foreColor = TextStyle.COLOR_INDEX_DEFAULT
                        40, 41, 42, 43, 44, 45, 46, 47 -> style.backColor = param - 40
                        49 -> style.backColor = TextStyle.COLOR_INDEX_DEFAULT
                        90, 91, 92, 93, 94, 95, 96, 97 -> style.foreColor = param - 90 + 8
                        100, 101, 102, 103, 104, 105, 106, 107 -> style.backColor = param - 100 + 8
                    }
                }
            }
            
            // Device Status Report
            'n' -> {
                val report = parseParameter(params, 0)
                // DSR - Device Status Report
                // Will respond with cursor position
            }
            
            // DECSET/DECRST modes
            'h', 'l' -> {
                val param = params.toIntOrNull() ?: 0
                val set = lastChar == 'h'
                when (param) {
                    25 -> { /* Cursor visible */ }
                    1049 -> { /* Alternate screen buffer */ }
                    47, 1047 -> { /* Alternate screen buffer */ }
                    1048 -> { /* Save/restore cursor */ }
                }
            }
            
            else -> return false
        }
        return true
    }

    private fun processOscSeq(): Boolean {
        if (mParseBuffer.length < 3) return false
        if (mParseBuffer[1] != ']') return false
        
        // OSC sequences end with BEL (0x07) or ST (ESC \)
        val endIndex = mParseBuffer.indexOf(0x07.toChar())
        if (endIndex == -1) {
            val stIndex = mParseBuffer.indexOf("\u001B\\")
            if (stIndex == -1) return false
        }
        
        return true // OSC sequences are handled, but we don't need full implementation
    }

    private fun parseParameter(params: String, default: Int): Int {
        return if (params.isEmpty()) {
            default
        } else {
            params.split(';').firstOrNull()?.toIntOrNull() ?: default
        }
    }

    private fun updateCurrentScreen() {
        // Copy buffer to current screen
        for (row in 0 until mRows) {
            for (col in 0 until mColumns) {
                mCurrentScreen[row][col].copyFrom(mBuffer.getCell(row, col))
            }
        }
        // Mark cursor position
        mCurrentScreen[mBuffer.cursorRow][mBuffer.cursorCol].cursor = true
        
        _cursorPosition.value = Pair(mBuffer.cursorRow, mBuffer.cursorCol)
        _screenUpdates.value = mCurrentScreen.map { it.toList() }
    }

    fun getScreenText(): String {
        val sb = StringBuilder()
        for (row in 0 until mRows) {
            for (col in 0 until mColumns) {
                sb.append(mBuffer.getCell(row, col).char)
            }
            if (row < mRows - 1) sb.append('\n')
        }
        return sb.toString()
    }

    fun getTotalScrollCount(): Int = mTotalScroll

    fun receiveDa1Response(): String = "\u001B[?1;0c"
}
