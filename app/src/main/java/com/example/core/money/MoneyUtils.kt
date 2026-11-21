package com.example.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyUtils {
    val supportedCurrencies = listOf("INR", "BDT", "USD", "EUR", "GBP")

    fun currencySymbol(code: String): String = when (code.uppercase(Locale.US)) {
        "INR" -> "₹"
        "BDT" -> "৳"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> code.uppercase(Locale.US) + " "
    }

    /**
     * Parses a decimal string (e.g. "500", "1250.50") into exact integer minor units (paise/poisha/cents)
     * using BigDecimal arithmetic — never floating-point Double.
     */
    fun parseToMinor(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            if (bd <= BigDecimal.ZERO) return null
            val minor = bd.multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
            val result = minor.longValueExact()
            if (result in 1L..100_000_000_000L) result else null
        } catch (_: Exception) {
            null
        }
    }

    fun formatMinor(amountMinor: Long, currencyCode: String, signed: Boolean = false): String {
        val bd = BigDecimal.valueOf(amountMinor, 2)
        val absBd = bd.abs()
        val nf = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = if (absBd.stripTrailingZeros().scale() <= 0) 0 else 2
            maximumFractionDigits = 2
        }
        val formattedNumber = nf.format(absBd)
        val symbol = currencySymbol(currencyCode)
        return when {
            amountMinor < 0 -> "-$symbol$formattedNumber"
            signed && amountMinor > 0 -> "+$symbol$formattedNumber"
            else -> "$symbol$formattedNumber"
        }
    }

    fun minorToDecimalString(amountMinor: Long): String {
        return BigDecimal.valueOf(amountMinor, 2).toPlainString()
    }
}
