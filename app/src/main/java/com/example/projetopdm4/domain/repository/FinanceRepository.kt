package com.example.projetopdm4.domain.repository

import com.example.projetopdm4.domain.model.AuthSession
import com.example.projetopdm4.domain.model.CategoryBudget
import com.example.projetopdm4.domain.model.FinanceCategory
import com.example.projetopdm4.domain.model.FinanceData
import com.example.projetopdm4.domain.model.FinanceTransaction
import com.example.projetopdm4.domain.model.SavingsGoal
import kotlinx.coroutines.flow.StateFlow

interface FinanceRepository {
    val data: StateFlow<FinanceData>
    val syncStatus: StateFlow<String>
    val authState: StateFlow<AuthSession>

    fun startSync()
    suspend fun saveTransaction(transaction: FinanceTransaction)
    suspend fun deleteTransaction(id: Long)
    suspend fun saveCategory(name: String, type: String)
    suspend fun deleteCategory(id: String)
    suspend fun saveBudget(budget: CategoryBudget)
    suspend fun deleteBudget(categoryId: String, month: String)
    suspend fun saveGoal(goal: SavingsGoal)
    suspend fun deleteGoal(id: String)
    suspend fun signIn(email: String, password: String)
    suspend fun createAccount(email: String, password: String)
    fun signOut()
    suspend fun updateDisplayName(name: String)
}
