package com.example.projetopdm4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projetopdm4.domain.model.FinanceTransaction
import com.example.projetopdm4.presentation.viewmodel.FinanceUiState
import com.example.projetopdm4.presentation.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
internal fun TransactionsScreen(state: FinanceUiState, viewModel: FinanceViewModel) {
    var startText by rememberSaveable { mutableStateOf("") }
    var endText by rememberSaveable { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FinanceTransaction?>(null) }
    var selectedForDelete by remember { mutableStateOf<FinanceTransaction?>(null) }
    val start = parseDate(startText)?.time
    val end = parseDate(endText)?.let { date ->
        Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
    }
    val filtered = state.transactionsBetween(start, end)

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Movimentações", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(startText, { startText = it }, label = { Text("De (dd/MM/aaaa)") }, singleLine = true)
            OutlinedTextField(endText, { endText = it }, label = { Text("Até (dd/MM/aaaa)") }, singleLine = true)
            if ((startText.isNotBlank() && start == null) || (endText.isNotBlank() && end == null)) {
                Text("Use datas válidas no formato dd/MM/aaaa.", color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showForm = true }) { Text("＋ Novo lançamento") }
                Text("${filtered.size} lançamentos", modifier = Modifier.padding(top = 12.dp))
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered, key = { it.id }) { item ->
                TransactionRow(
                    item,
                    onClick = { editing = item },
                    onLongClick = { selectedForDelete = item }
                )
            }
            if (filtered.isEmpty()) item { Text("Nenhum lançamento neste período.") }
            item { Text("Toque para editar; mantenha pressionado para excluir.", style = MaterialTheme.typography.bodySmall) }
        }
    }

    val formTransaction = editing
    if (showForm || formTransaction != null) {
        TransactionDialog(
            categories = state.data.categories,
            initialType = EXPENSE,
            existing = formTransaction,
            onDismiss = { showForm = false; editing = null },
            onSave = { type, amount, category, note, date ->
                viewModel.saveTransaction(formTransaction, type, amount, category, note, date)
                showForm = false
                editing = null
            }
        )
    }
    selectedForDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedForDelete = null },
            title = { Text("Excluir lançamento?") },
            text = { Text("${item.categoryName} · ${formatMoney(item.amountCents)}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(item.id)
                    selectedForDelete = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { selectedForDelete = null }) { Text("Cancelar") } }
        )
    }
}
