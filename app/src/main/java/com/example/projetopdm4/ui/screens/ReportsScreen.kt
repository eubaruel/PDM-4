package com.example.projetopdm4.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projetopdm4.data.export.FinanceReportExporter
import com.example.projetopdm4.presentation.viewmodel.FinanceUiState

@Composable
internal fun ReportsScreen(state: FinanceUiState) {
    val context = LocalContext.current
    val currentMonth = state.currentMonth
    val monthTransactions = state.currentMonthTransactions
    val incomes = state.monthlyIncomeCents
    val expenses = state.monthlyExpenseCents
    val grouped = state.expensesByCategory

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Não foi possível abrir o arquivo.")
                output.use { FinanceReportExporter.writeCsv(it, state.data.transactions) }
            }.onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show() }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Não foi possível abrir o arquivo.")
                output.use {
                    FinanceReportExporter.writePdf(it, currentMonth, monthTransactions, incomes, expenses)
                }
            }.onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show() }
        }
    }

    ScreenColumn {
        Text("Relatórios", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(currentMonth, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Receitas", incomes, Modifier.weight(1f), Color(0xFF167D52))
            SummaryCard("Despesas", expenses, Modifier.weight(1f), MaterialTheme.colorScheme.error)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Despesas por categoria", style = MaterialTheme.typography.titleLarge)
                if (grouped.isEmpty()) {
                    Text("Sem despesas neste mês.")
                } else {
                    SpendingChart(grouped.map { it.second }, MaterialTheme.colorScheme.primary)
                    grouped.forEach { (name, amount) ->
                        Text("$name · ${formatMoney(amount)}")
                    }
                }
            }
        }
        Text("Saldo geral: ${formatMoney(state.totalBalanceCents)}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { csvLauncher.launch("relatorio-financeiro.csv") }) { Text("Exportar CSV") }
            OutlinedButton(onClick = { pdfLauncher.launch("relatorio-financeiro.pdf") }) { Text("Exportar PDF") }
        }
        Text(state.syncStatus, style = MaterialTheme.typography.bodySmall)
    }
}
