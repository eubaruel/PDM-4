package com.example.projetopdm4.data.local

import com.example.projetopdm4.domain.model.CategoryBudget
import com.example.projetopdm4.domain.model.FinanceCategory
import com.example.projetopdm4.domain.model.FinanceTransaction
import com.example.projetopdm4.domain.model.SavingsGoal

fun FinanceTransactionEntity.toDomain() = FinanceTransaction(
    id, type, amountCents, categoryId, categoryName, description, occurredAt
)

fun FinanceTransaction.toEntity() = FinanceTransactionEntity(
    id, type, amountCents, categoryId, categoryName, description, occurredAt
)

fun FinanceCategoryEntity.toDomain() = FinanceCategory(id, name, type)
fun FinanceCategory.toEntity() = FinanceCategoryEntity(id, name, type)

fun CategoryBudgetEntity.toDomain() = CategoryBudget(categoryId, categoryName, month, limitCents)
fun CategoryBudget.toEntity() = CategoryBudgetEntity(categoryId, categoryName, month, limitCents)

fun SavingsGoalEntity.toDomain() = SavingsGoal(id, name, targetCents, savedCents, targetDate)
fun SavingsGoal.toEntity() = SavingsGoalEntity(id, name, targetCents, savedCents, targetDate)
