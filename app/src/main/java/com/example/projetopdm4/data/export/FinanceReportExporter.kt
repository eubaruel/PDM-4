package com.example.projetopdm4.data.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.projetopdm4.domain.model.FinanceTransaction
import java.io.OutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinanceReportExporter {
    fun writeCsv(output: OutputStream, transactions: List<FinanceTransaction>) {
        output.bufferedWriter().use { writer ->
            writer.appendLine("data,tipo,categoria,descricao,valor")
            transactions.sortedBy { it.occurredAt }.forEach { transaction ->
                writer.appendLine(
                    listOf(
                        csv(dateLabel(transaction.occurredAt)),
                        csv(transaction.type.toTypeLabel()),
                        csv(transaction.categoryName),
                        csv(transaction.description),
                        csv(centsToInput(transaction.amountCents))
                    ).joinToString(",")
                )
            }
        }
    }

    fun writePdf(
        output: OutputStream,
        month: String,
        transactions: List<FinanceTransaction>,
        incomes: Long,
        expenses: Long
    ) {
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val paint = Paint().apply { textSize = 14f }
            val canvas = page.canvas
            canvas.drawText("Relatório financeiro - $month", 40f, 50f, paint)
            canvas.drawText("Receitas: ${formatMoney(incomes)}", 40f, 80f, paint)
            canvas.drawText("Despesas: ${formatMoney(expenses)}", 40f, 105f, paint)
            var y = 145f
            transactions.sortedByDescending { it.occurredAt }.take(35).forEach { transaction ->
                val row = "${dateLabel(transaction.occurredAt)} | ${transaction.type.toTypeLabel()} | " +
                    "${transaction.categoryName} | ${formatMoney(transaction.amountCents)}"
                canvas.drawText(row.take(70), 40f, y, paint)
                y += 19f
            }
            if (transactions.size > 35) {
                canvas.drawText("PDF mostra os 35 lançamentos mais recentes.", 40f, 820f, paint)
            }
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""

    private fun centsToInput(cents: Long) =
        BigDecimal.valueOf(cents, 2).toPlainString().replace('.', ',')

    private fun formatMoney(cents: Long) =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
            .format(BigDecimal.valueOf(cents, 2))

    private fun dateLabel(timestamp: Long) =
        SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).format(Date(timestamp))

    private fun String.toTypeLabel() = if (this == "INCOME") "Receita" else "Despesa"
}
