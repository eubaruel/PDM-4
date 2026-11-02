package com.example.projetopdm4.domain.model

data class FinanceTransaction(
    val id: Long = 0,
    val type: String,
    val amountCents: Long,
    val categoryId: String,
    val categoryName: String,
    val description: String,
    val occurredAt: Long
)

data class FinanceCategory(
    val id: String,
    val name: String,
    val type: String
)

data class CategoryBudget(
    val categoryId: String,
    val categoryName: String,
    val month: String,
    val limitCents: Long
)

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetCents: Long,
    val savedCents: Long,
    val targetDate: Long?
)

data class FinanceData(
    val transactions: List<FinanceTransaction> = emptyList(),
    val categories: List<FinanceCategory> = emptyList(),
    val budgets: List<CategoryBudget> = emptyList(),
    val goals: List<SavingsGoal> = emptyList()
)

data class AuthSession(
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val isAnonymous: Boolean = true
)
