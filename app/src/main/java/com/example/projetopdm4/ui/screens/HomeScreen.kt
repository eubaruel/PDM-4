package com.example.projetopdm4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projetopdm4.domain.model.FinanceTransaction
import com.example.projetopdm4.presentation.viewmodel.FinanceUiState
import com.example.projetopdm4.presentation.viewmodel.FinanceViewModel

@Composable
internal fun HomeScreen(state: FinanceUiState, viewModel: FinanceViewModel) {
    var showForm by remember { mutableStateOf(false) }
    var newType by remember { mutableStateOf(EXPENSE) }
    var editing by remember { mutableStateOf<FinanceTransaction?>(null) }
    val transactions = state.data.transactions

    ScreenColumn {
        Text("Meu dinheiro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(state.syncStatus, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Saldo disponível")
                Text(formatMoney(state.totalBalanceCents), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Receitas do mês", state.monthlyIncomeCents, Modifier.weight(1f), Color(0xFF167D52))
            SummaryCard("Despesas do mês", state.monthlyExpenseCents, Modifier.weight(1f), MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { newType = INCOME; showForm = true }, modifier = Modifier.weight(1f)) {
                Text("＋ Receita")
            }
            OutlinedButton(onClick = { newType = EXPENSE; showForm = true }, modifier = Modifier.weight(1f)) {
                Text("− Despesa")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Últimas movimentações", style = MaterialTheme.typography.titleLarge)
        transactions.take(5).forEach { TransactionRow(it, onClick = { editing = it }) }
    }
    val formTransaction = editing
    if (showForm || formTransaction != null) {
        TransactionDialog(
            categories = state.data.categories,
            existing = formTransaction,
            initialType = newType,
            onDismiss = { showForm = false; editing = null },
            onSave = { type, amount, category, note, date ->
                viewModel.saveTransaction(formTransaction, type, amount, category, note, date)
                showForm = false
                editing = null
            }
        )
    }
}
