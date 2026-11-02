package com.example.projetopdm4.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projetopdm4.domain.model.FinanceCategory
import com.example.projetopdm4.domain.model.SavingsGoal
import com.example.projetopdm4.presentation.viewmodel.FinanceUiState
import com.example.projetopdm4.presentation.viewmodel.FinanceViewModel

@Composable
internal fun PlanningScreen(state: FinanceUiState, viewModel: FinanceViewModel) {
    var budgetCategory by remember { mutableStateOf<FinanceCategory?>(null) }
    var budgetAmount by rememberSaveable { mutableStateOf("") }
    var budgetMonth by rememberSaveable { mutableStateOf(currentMonthKey()) }
    var goalName by rememberSaveable { mutableStateOf("") }
    var goalTarget by rememberSaveable { mutableStateOf("") }
    var goalSaved by rememberSaveable { mutableStateOf("0") }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showGoalForm by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenColumn {
        Text("Planejamento", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Organize seus limites mensais e objetivos.")
        Text("Orçamentos mensais", style = MaterialTheme.typography.titleLarge)
        val expenseCategories = state.data.categories.filter { it.type == EXPENSE }
        CategoryPicker(expenseCategories, budgetCategory?.takeIf { selected ->
            expenseCategories.any { it.id == selected.id }
        }) {
            budgetCategory = it
        }
        OutlinedTextField(budgetAmount, { budgetAmount = it }, label = { Text("Limite (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(budgetMonth, { budgetMonth = it }, label = { Text("Mês (AAAA-MM)") }, singleLine = true)
        Button(onClick = {
            val amount = parseCents(budgetAmount)
            val category = state.data.categories.firstOrNull { it.id == budgetCategory?.id && it.type == EXPENSE }
            if (category == null || amount == null ||
                !Regex("\\d{4}-(0[1-9]|1[0-2])").matches(budgetMonth)
            ) {
                error = "Escolha uma categoria e informe limite e mês válidos."
            } else {
                viewModel.saveBudget(category, budgetMonth, amount)
                budgetAmount = ""
                error = null
            }
        }) { Text("Salvar orçamento") }
        state.data.budgets.forEach { budget ->
            val spent = state.budgetSpent(budget.categoryId, budget.month)
            TextButton(onClick = { viewModel.deleteBudget(budget.categoryId, budget.month) }) {
                Text("${budget.categoryName} · ${budget.month}: ${formatMoney(spent)} / ${formatMoney(budget.limitCents)} (remover)")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Metas financeiras", style = MaterialTheme.typography.titleLarge)
        Button(onClick = {
            editingGoal = null
            goalName = ""
            goalTarget = ""
            goalSaved = "0"
            showGoalForm = true
        }) { Text("＋ Nova meta") }
        state.data.goals.forEach { goal ->
            val progress = if (goal.targetCents > 0) (goal.savedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f) else 0f
            Card(Modifier.fillMaxWidth().clickable { editingGoal = goal; showGoalForm = true }) {
                Column(Modifier.padding(14.dp)) {
                    Text(goal.name, fontWeight = FontWeight.Bold)
                    Text("${formatMoney(goal.savedCents)} de ${formatMoney(goal.targetCents)} (${(progress * 100).toInt()}%)")
                    TextButton(onClick = { viewModel.deleteGoal(goal.id) }) { Text("Excluir meta") }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    if (showGoalForm) {
        val goal = editingGoal
        AlertDialog(
            onDismissRequest = { showGoalForm = false },
            title = { Text(if (goal == null) "Nova meta" else "Editar meta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(goalName, { goalName = it }, label = { Text("Nome") }, singleLine = true)
                    OutlinedTextField(goalTarget, { goalTarget = it }, label = { Text("Valor da meta (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(goalSaved, { goalSaved = it }, label = { Text("Já guardado (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = parseCents(goalTarget)
                    val saved = parseCents(goalSaved)
                    if (goalName.isNotBlank() && target != null && saved != null) {
                        viewModel.saveGoal(goal, goalName, target, saved)
                        goalName = ""
                        goalTarget = ""
                        goalSaved = "0"
                        showGoalForm = false
                    } else error = "Preencha nome e valores válidos."
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showGoalForm = false }) { Text("Cancelar") }
            }
        )
        if (goal != null) {
            LaunchedFillGoal(goal) {
                goalName = it.name
                goalTarget = centsToInput(it.targetCents)
                goalSaved = centsToInput(it.savedCents)
            }
        }
    }
}
