package com.example.projetopdm4.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.projetopdm4.domain.model.FinanceCategory
import com.example.projetopdm4.domain.model.FinanceTransaction
import com.example.projetopdm4.domain.model.SavingsGoal

@Composable
internal fun TransactionDialog(
    categories: List<FinanceCategory>,
    existing: FinanceTransaction? = null,
    initialType: String = EXPENSE,
    onDismiss: () -> Unit,
    onSave: (String, Long, FinanceCategory, String, Long) -> Unit
) {
    var type by remember(existing, initialType) { mutableStateOf(existing?.type ?: initialType) }
    var amount by remember(existing) { mutableStateOf(existing?.let { centsToInput(it.amountCents) }.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var date by remember(existing) { mutableStateOf(existing?.let { dateLabel(it.occurredAt) } ?: dateLabel(System.currentTimeMillis())) }
    val availableCategories = remember(categories, existing) {
        if (existing != null && categories.none { it.id == existing.categoryId }) {
            categories + FinanceCategory(existing.categoryId, existing.categoryName, existing.type)
        } else {
            categories
        }
    }
    var category by remember(existing, availableCategories) {
        mutableStateOf(availableCategories.firstOrNull { it.id == existing?.categoryId && it.type == type }
            ?: availableCategories.firstOrNull { it.type == type })
    }
    var error by remember { mutableStateOf<String?>(null) }
    val validCategories = availableCategories.filter { it.type == type }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Novo lançamento" else "Editar lançamento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (type == INCOME) Button(onClick = { type = INCOME }) { Text("Receita") }
                    else OutlinedButton(onClick = {
                        type = INCOME
                        category = availableCategories.firstOrNull { it.type == INCOME }
                    }) { Text("Receita") }
                    if (type == EXPENSE) Button(onClick = { type = EXPENSE }) { Text("Despesa") }
                    else OutlinedButton(onClick = {
                        type = EXPENSE
                        category = availableCategories.firstOrNull { it.type == EXPENSE }
                    }) { Text("Despesa") }
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("Valor (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                CategoryPicker(validCategories, category) { category = it }
                if (validCategories.isEmpty()) {
                    Text("Crie uma categoria deste tipo em Configurações.")
                }
                OutlinedTextField(description, { description = it }, label = { Text("Descrição (opcional)") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Data (dd/MM/aaaa)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = parseCents(amount)
                val parsedDate = parseDate(date)
                val selected = category
                if (parsedAmount == null || parsedAmount <= 0 || parsedDate == null || selected == null) {
                    error = "Informe valor, data e uma categoria válida."
                } else {
                    onSave(type, parsedAmount, selected, description.trim(), parsedDate.time)
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
internal fun CategoryPicker(
    categories: List<FinanceCategory>,
    selected: FinanceCategory?,
    onSelect: (FinanceCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected?.name ?: if (categories.isEmpty()) "Crie uma categoria primeiro" else "Escolher categoria")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = { onSelect(category); expanded = false }
                )
            }
        }
    }
}

@Composable
internal fun SummaryCard(title: String, cents: Long, modifier: Modifier, color: Color) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(formatMoney(cents), color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun TransactionRow(
    item: FinanceTransaction,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val amountColor = if (item.type == INCOME) Color(0xFF167D52) else MaterialTheme.colorScheme.error
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.categoryName, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(item.description.takeIf { it.isNotBlank() }, dateLabel(item.occurredAt))
                        .filterNotNull().joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "${if (item.type == INCOME) "+" else "−"}${formatMoney(item.amountCents)}",
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun SpendingChart(values: List<Long>, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(160.dp).padding(vertical = 12.dp)) {
        if (values.isEmpty()) return@Canvas
        val max = (values.maxOrNull() ?: 1L).toFloat().coerceAtLeast(1f)
        val gap = 10.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(2.dp.toPx())
        values.forEachIndexed { index, value ->
            val barHeight = (size.height * value / max).coerceAtLeast(3.dp.toPx())
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(index * (barWidth + gap), size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
internal fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
internal fun LaunchedFillGoal(goal: SavingsGoal, onReady: (SavingsGoal) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(goal.id) { onReady(goal) }
}
