package com.example.projetopdm4.ui.screens

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun parseCents(value: String): Long? = value.trim()
    .replace("R$", "", ignoreCase = true)
    .replace(" ", "")
    .let { normalized ->
        if (normalized.contains(',')) normalized.replace(".", "").replace(',', '.')
        else normalized
    }
    .toBigDecimalOrNull()
    ?.takeIf { it > BigDecimal.ZERO }
    ?.let {
        runCatching { it.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact() }
            .getOrNull()
    }

internal fun centsToInput(cents: Long): String =
    BigDecimal.valueOf(cents, 2).setScale(2).toPlainString().replace('.', ',')

internal fun formatMoney(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        .format(BigDecimal.valueOf(cents, 2))

internal fun dateLabel(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).format(Date(timestamp))

internal fun parseDate(value: String): Date? {
    val text = value.trim()
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).apply {
        isLenient = false
    }
    val position = ParsePosition(0)
    val parsed = formatter.parse(text, position) ?: return null
    return parsed.takeIf { position.index == text.length }
}

internal fun monthKey(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM", Locale.ROOT).format(Date(timestamp))

internal fun currentMonthKey(): String = monthKey(System.currentTimeMillis())

internal fun String.toTypeLabel(): String = if (this == INCOME) "Receita" else "Despesa"
