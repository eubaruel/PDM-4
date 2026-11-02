package com.example.projetopdm4.domain.usecase

import com.example.projetopdm4.domain.model.CategoryBudget
import com.example.projetopdm4.domain.model.FinanceCategory
import com.example.projetopdm4.domain.model.FinanceTransaction
import com.example.projetopdm4.domain.model.SavingsGoal
import com.example.projetopdm4.domain.repository.FinanceRepository
import java.util.UUID

class FinanceUseCases(private val repository: FinanceRepository) {
    val data = repository.data
    val syncStatus = repository.syncStatus
    val authState = repository.authState

    fun startSync() = repository.startSync()

    suspend fun saveTransaction(
        existing: FinanceTransaction?,
        type: String,
        amountCents: Long,
        category: FinanceCategory,
        description: String,
        occurredAt: Long
    ) = repository.saveTransaction(
        FinanceTransaction(
            id = existing?.id ?: 0,
            type = type,
            amountCents = amountCents,
            categoryId = category.id,
            categoryName = category.name,
            description = description,
            occurredAt = occurredAt
        )
    )

    suspend fun deleteTransaction(id: Long) = repository.deleteTransaction(id)
    suspend fun saveCategory(name: String, type: String) = repository.saveCategory(name.trim(), type)
    suspend fun deleteCategory(id: String) = repository.deleteCategory(id)

    suspend fun saveBudget(category: FinanceCategory, month: String, limitCents: Long) =
        repository.saveBudget(CategoryBudget(category.id, category.name, month, limitCents))

    suspend fun deleteBudget(categoryId: String, month: String) =
        repository.deleteBudget(categoryId, month)

    suspend fun saveGoal(existing: SavingsGoal?, name: String, targetCents: Long, savedCents: Long) =
        repository.saveGoal(
            SavingsGoal(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                targetCents = targetCents,
                savedCents = savedCents,
                targetDate = existing?.targetDate
            )
        )

    suspend fun deleteGoal(id: String) = repository.deleteGoal(id)
    suspend fun signIn(email: String, password: String) = repository.signIn(email.trim(), password)
    suspend fun createAccount(email: String, password: String) =
        repository.createAccount(email.trim(), password)

    fun signOut() = repository.signOut()
    suspend fun updateDisplayName(name: String) = repository.updateDisplayName(name)
}
