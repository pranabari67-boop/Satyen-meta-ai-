package com.example

import com.example.core.money.MoneyUtils
import com.example.data.model.TransactionItem
import com.example.ui.viewmodel.MainLifeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyAndTotalsUnitTest {
    @Test
    fun parseToMinor_handlesExactDecimalsWithoutFloatingError() {
        assertEquals(50000L, MoneyUtils.parseToMinor("500"))
        assertEquals(125075L, MoneyUtils.parseToMinor("1,250.75"))
        assertEquals(10L, MoneyUtils.parseToMinor("0.10"))
        assertNull(MoneyUtils.parseToMinor("0"))
        assertNull(MoneyUtils.parseToMinor("-50"))
        assertNull(MoneyUtils.parseToMinor("abc"))
    }

    @Test
    fun formatMinor_formatsCurrenciesAccurately() {
        assertEquals("₹500", MoneyUtils.formatMinor(50000L, "INR"))
        assertEquals("৳1,250.50", MoneyUtils.formatMinor(125050L, "BDT"))
        assertEquals("+$100", MoneyUtils.formatMinor(10000L, "USD", signed = true))
        assertEquals("-$50", MoneyUtils.formatMinor(-5000L, "USD"))
    }

    @Test
    fun computeMonthlyTotals_excludesReversedAndReversalTransactions() {
        val now = System.currentTimeMillis()
        val list = listOf(
            TransactionItem(id = "tx1", type = "INCOME", amountMinor = 100000L, occurredAtMillis = now),
            TransactionItem(id = "tx2", type = "EXPENSE", amountMinor = 30000L, occurredAtMillis = now),
            TransactionItem(id = "tx3", type = "EXPENSE", amountMinor = 20000L, occurredAtMillis = now),
            TransactionItem(id = "tx4", type = "REVERSAL", amountMinor = 20000L, reversedTransactionId = "tx3", occurredAtMillis = now)
        )
        val (inc, exp) = MainLifeViewModel.computeMonthlyTotals(list)
        assertEquals(100000L, inc)
        assertEquals(30000L, exp)
    }
}
