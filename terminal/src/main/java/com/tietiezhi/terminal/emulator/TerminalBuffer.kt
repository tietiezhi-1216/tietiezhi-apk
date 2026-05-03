package com.tietiezhi.terminal.emulator

import androidx.compose.ui.graphics.Color

/**
 * Terminal cell holding character and style information
 */
data class Cell(
    var char: Char = ' ',
    var foreColor: Int = TextStyle.COLOR_INDEX_WHITE,
    var backColor: Int = TextStyle.COLOR_INDEX_BLACK,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underline: Boolean = false,
    var inverse: Boolean = false,
    var strikethrough: Boolean = false,
    var concealed: Boolean = false,
    var cursor: Boolean = false
) {
    fun copyFrom(other: Cell) {
        char = other.char
        foreColor = other.foreColor
        backColor = other.backColor
        bold = other.bold
        italic = other.italic
        underline = other.underline
        inverse = other.inverse
        strikethrough = other.strikethrough
        concealed = other.concealed
        cursor = other.cursor
    }

    fun clear() {
        char = ' '
        foreColor = TextStyle.COLOR_INDEX_WHITE
        backColor = TextStyle.COLOR_INDEX_BLACK
        bold = false
        italic = false
        underline = false
        inverse = false
        strikethrough = false
        concealed = false
    }
}

/**
 * Text style constants for ANSI colors
 */
object TextStyle {
    const val COLOR_INDEX_BLACK = 0
    const val COLOR_INDEX_RED = 1
    const val COLOR_INDEX_GREEN = 2
    const val COLOR_INDEX_YELLOW = 3
    const val COLOR_INDEX_BLUE = 4
    const val COLOR_INDEX_MAGENTA = 5
    const val COLOR_INDEX_CYAN = 6
    const val COLOR_INDEX_WHITE = 7
    const val COLOR_INDEX_DEFAULT = 9
    
    // Bright colors
    const val COLOR_INDEX_BRIGHT_BLACK = 8
    const val COLOR_INDEX_BRIGHT_RED = 9
    const val COLOR_INDEX_BRIGHT_GREEN = 10
    const val COLOR_INDEX_BRIGHT_YELLOW = 11
    const val COLOR_INDEX_BRIGHT_BLUE = 12
    const val COLOR_INDEX_BRIGHT_MAGENTA = 13
    const val COLOR_INDEX_BRIGHT_CYAN = 14
    const val COLOR_INDEX_BRIGHT_WHITE = 15

    val ANSI_COLORS = arrayOf(
        Color(0xFF000000), // Black
        Color(0xFFCD3131), // Red
        Color(0xFF0DBC79), // Green
        Color(0xFFE5E510), // Yellow
        Color(0xFF2472C8), // Blue
        Color(0xFFBC3FBC), // Magenta
        Color(0xFF11A8CD), // Cyan
        Color(0xFFE5E5E5), // White
        Color(0xFF666666), // Bright Black (Gray)
        Color(0xFFF14C4C), // Bright Red
        Color(0xFF23D18B), // Bright Green
        Color(0xFFF5F543), // Bright Yellow
        Color(0xFF3B8EEA), // Bright Blue
        Color(0xFFD670D6), // Bright Magenta
        Color(0xFF29B8DB), // Bright Cyan
        Color(0xFFFFFFFF)  // Bright White
    )

    val DEFAULT_FORE_COLOR = ANSI_COLORS[COLOR_INDEX_WHITE]
    val DEFAULT_BACK_COLOR = ANSI_COLORS[COLOR_INDEX_BLACK]
}

/**
 * Terminal screen buffer holding all character cells
 */
class TerminalBuffer(var columns: Int, var rows: Int) {
    private var cells: Array<Array<Cell>> = Array(rows) { Array(columns) { Cell() } }
    
    var cursorRow: Int = 0
        private set
    var cursorCol: Int = 0
        private set
    
    var scrollRegionTop: Int = 0
        private set
    var scrollRegionBottom: Int = rows - 1
        private set
    
    private val defaultStyle = CurrentStyle()
    
    fun getCell(row: Int, col: Int): Cell {
        return if (row in 0 until rows && col in 0 until columns) {
            cells[row][col]
        } else {
            Cell()
        }
    }

    fun setCursor(row: Int, col: Int) {
        cursorRow = row.coerceIn(0, rows - 1)
        cursorCol = col.coerceIn(0, columns - 1)
    }

    fun moveCursor(rowDelta: Int, colDelta: Int) {
        setCursor(cursorRow + rowDelta, cursorCol + colDelta)
    }

    fun setScrollRegion(top: Int, bottom: Int) {
        scrollRegionTop = top.coerceIn(0, rows - 1)
        scrollRegionBottom = bottom.coerceIn(top, rows - 1)
    }

    fun resetScrollRegion() {
        scrollRegionTop = 0
        scrollRegionBottom = rows - 1
    }

    fun reset() {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                cells[row][col].clear()
            }
        }
        cursorRow = 0
        cursorCol = 0
        defaultStyle.reset()
        resetScrollRegion()
    }

    fun resize(newColumns: Int, newRows: Int) {
        val newCells = Array(newRows) { Array(newColumns) { Cell() } }
        
        val minRows = minOf(rows, newRows)
        val minCols = minOf(columns, newColumns)
        
        for (row in 0 until minRows) {
            for (col in 0 until minCols) {
                newCells[row][col].copyFrom(cells[row][col])
            }
        }
        
        // Update cursor position if needed
        cursorRow = cursorRow.coerceIn(0, newRows - 1)
        cursorCol = cursorCol.coerceIn(0, newColumns - 1)
        
        scrollRegionTop = scrollRegionTop.coerceIn(0, newRows - 1)
        scrollRegionBottom = scrollRegionBottom.coerceIn(scrollRegionTop, newRows - 1)
        
        // Update dimensions and cells
        columns = newColumns
        rows = newRows
        cells = newCells
    }

    fun insertChars(count: Int) {
        val row = cursorRow
        val numChars = count.coerceAtMost(columns - cursorCol)
        for (col in (columns - count - 1).coerceAtLeast(cursorCol) until columns - 1) {
            cells[row][col + numChars].copyFrom(cells[row][col])
        }
        for (col in cursorCol until cursorCol + numChars) {
            cells[row][col].clear()
        }
    }

    fun deleteChars(count: Int) {
        val row = cursorRow
        val numChars = count.coerceAtMost(columns - cursorCol)
        for (col in cursorCol until columns - numChars) {
            cells[row][col].copyFrom(cells[row][col + numChars])
        }
        for (col in (columns - numChars) until columns) {
            cells[row][col].clear()
        }
    }

    fun insertLines(count: Int) {
        val numLines = count.coerceAtMost(scrollRegionBottom - cursorRow + 1)
        for (row in scrollRegionBottom - numLines downTo cursorRow) {
            for (col in 0 until columns) {
                cells[row + numLines][col].copyFrom(cells[row][col])
            }
        }
        for (row in cursorRow until cursorRow + numLines) {
            for (col in 0 until columns) {
                cells[row][col].clear()
            }
        }
    }

    fun deleteLines(count: Int) {
        val numLines = count.coerceAtMost(scrollRegionBottom - cursorRow + 1)
        for (row in cursorRow until scrollRegionBottom - numLines + 1) {
            for (col in 0 until columns) {
                cells[row][col].copyFrom(cells[row + numLines][col])
            }
        }
        for (row in scrollRegionBottom - numLines + 1..scrollRegionBottom) {
            for (col in 0 until columns) {
                cells[row][col].clear()
            }
        }
    }

    fun scrollUp() {
        for (row in scrollRegionTop until scrollRegionBottom) {
            for (col in 0 until columns) {
                cells[row][col].copyFrom(cells[row + 1][col])
            }
        }
        for (col in 0 until columns) {
            cells[scrollRegionBottom][col].clear()
        }
    }

    fun scrollDown() {
        for (row in scrollRegionBottom downTo scrollRegionTop + 1) {
            for (col in 0 until columns) {
                cells[row][col].copyFrom(cells[row - 1][col])
            }
        }
        for (col in 0 until columns) {
            cells[scrollRegionTop][col].clear()
        }
    }

    fun eraseChars(startCol: Int, endCol: Int, row: Int) {
        for (col in startCol..endCol) {
            if (col in 0 until columns) {
                cells[row][col].clear()
            }
        }
    }

    fun eraseLine(mode: Int) {
        when (mode) {
            0 -> eraseChars(cursorCol, columns - 1, cursorRow)
            1 -> eraseChars(0, cursorCol, cursorRow)
            2 -> eraseChars(0, columns - 1, cursorRow)
        }
    }

    fun eraseDisplay(mode: Int) {
        when (mode) {
            0 -> {
                eraseChars(cursorCol, columns - 1, cursorRow)
                for (row in (cursorRow + 1) until rows) {
                    eraseChars(0, columns - 1, row)
                }
            }
            1 -> {
                for (row in 0 until cursorRow) {
                    eraseChars(0, columns - 1, row)
                }
                eraseChars(0, cursorCol, cursorRow)
            }
            2, 3 -> {
                for (row in 0 until rows) {
                    eraseChars(0, columns - 1, row)
                }
                setCursor(0, 0)
            }
        }
    }

    fun getVisibleLines(startRow: Int, count: Int): List<List<Cell>> {
        return (startRow until (startRow + count).coerceAtMost(rows)).map { row ->
            cells[row].toList()
        }
    }

    fun getCurrentStyle(): CurrentStyle = defaultStyle
}

/**
 * Current style state for ANSI escape sequences
 */
class CurrentStyle {
    var foreColor: Int = TextStyle.COLOR_INDEX_DEFAULT
    var backColor: Int = TextStyle.COLOR_INDEX_DEFAULT
    var bold: Boolean = false
    var italic: Boolean = false
    var underline: Boolean = false
    var inverse: Boolean = false
    var strikethrough: Boolean = false
    var blinking: Boolean = false
    var concealed: Boolean = false

    fun reset() {
        foreColor = TextStyle.COLOR_INDEX_DEFAULT
        backColor = TextStyle.COLOR_INDEX_DEFAULT
        bold = false
        italic = false
        underline = false
        inverse = false
        strikethrough = false
        blinking = false
        concealed = false
    }

    fun applyTo(cell: Cell, defaultFore: Int, defaultBack: Int) {
        cell.foreColor = if (foreColor == TextStyle.COLOR_INDEX_DEFAULT) defaultFore else foreColor
        cell.backColor = if (backColor == TextStyle.COLOR_INDEX_DEFAULT) defaultBack else backColor
        cell.bold = bold
        cell.italic = italic
        cell.underline = underline
        cell.inverse = inverse
        cell.strikethrough = strikethrough
        cell.concealed = concealed
    }

    fun getForegroundColor(defaultColor: Int = TextStyle.COLOR_INDEX_WHITE): Int {
        return when (foreColor) {
            TextStyle.COLOR_INDEX_DEFAULT -> defaultColor
            in 0..7 -> foreColor
            in 8..15 -> foreColor - 8
            else -> defaultColor
        }
    }

    fun getBackgroundColor(defaultColor: Int = TextStyle.COLOR_INDEX_BLACK): Int {
        return when (backColor) {
            TextStyle.COLOR_INDEX_DEFAULT -> defaultColor
            in 0..7 -> backColor
            in 8..15 -> backColor - 8
            else -> defaultColor
        }
    }
}
