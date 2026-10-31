package com.smartmonitor.app

import android.content.Context

/**
 * Local watch-list for an arbitrary number of fleet vehicle codes.
 * This stores user-selected identifiers only; it does not fabricate live vehicle positions.
 */
class BusFleetWatchlist(context: Context) {
    private val prefs = context.getSharedPreferences("bus_fleet_watchlist", Context.MODE_PRIVATE)

    fun allCodes(): List<String> = prefs.getStringSet(KEY_CODES, setOf("۱۳۲۴"))
        .orEmpty()
        .map(::normalizeDigits)
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    fun add(rawCode: String): Boolean {
        val code = normalizeDigits(rawCode).filter(Char::isDigit)
        if (code.isBlank()) return false
        val updated = prefs.getStringSet(KEY_CODES, setOf("۱۳۲۴")).orEmpty().toMutableSet()
        updated.add(code)
        prefs.edit().putStringSet(KEY_CODES, updated).apply()
        return true
    }

    fun remove(code: String) {
        val updated = prefs.getStringSet(KEY_CODES, setOf("۱۳۲۴")).orEmpty().toMutableSet()
        updated.remove(normalizeDigits(code).filter(Char::isDigit))
        prefs.edit().putStringSet(KEY_CODES, updated).apply()
    }

    fun matchVisibleText(pageText: String): Set<String> {
        val normalizedText = normalizeDigits(pageText)
        return allCodes().filterTo(linkedSetOf()) { code ->
            Regex("(?<![0-9])${Regex.escape(code)}(?![0-9])").containsMatchIn(normalizedText)
        }
    }

    companion object {
        private const val KEY_CODES = "watched_bus_codes"

        fun normalizeDigits(value: String): String = buildString(value.length) {
            value.forEach { ch ->
                append(
                    when (ch) {
                        in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                        in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                        else -> ch
                    }
                )
            }
        }
    }
}
