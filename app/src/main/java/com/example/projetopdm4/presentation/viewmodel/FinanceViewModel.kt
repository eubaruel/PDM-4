package com.example.projetopdm4.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projetopdm4.domain.model.FinanceCategory
import com.example.projetopdm4.domain.model.FinanceData
import com.example.projetopdm4.domain.model.FinanceTransaction
import com.example.projetopdm4.domain.model.SavingsGoal
import com.example.projetopdm4.domain.repository.ProfileRepository
import com.example.projetopdm4.domain.usecase.FinanceUseCases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceViewModel(
    private val finance: FinanceUseCases,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val operationError = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)
    private val authBusy = MutableStateFlow(false)
    private val accountState = combine(
        finance.authState,
        profileRepository.profile,
        authError,
        authBusy
    ) { account, profile, loginError, busy ->
        AccountUiState(
            uid = account.uid,
            profileName = profile.name.ifBlank { account.displayName.orEmpty() },
            profileColor = profile.color,
            profilePhotoBase64 = profile.photoBase64,
            profileError = profile.error,
            email = account.email,
            isAnonymous = account.isAnonymous,
            authError = loginError,
            authBusy = busy
        )
    }
    val state = combine(
        finance.data,
        finance.syncStatus,
        operationError,
        accountState
    ) { data, syncStatus, error, account ->
        FinanceUiState(
            data = data,
            syncStatus = syncStatus,
            error = error,
            accountUid = account.uid,
            email = account.email,
            isAnonymous = account.isAnonymous,
            isAuthenticated = !account.isAnonymous && !account.email.isNullOrBlank(),
            profileName = account.profileName,
            profileColor = account.profileColor,
            profilePhotoBase64 = account.profilePhotoBase64,
            profileError = account.profileError,
            authError = account.authError,
            authBusy = account.authBusy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    init {
        finance.startSync()
        viewModelScope.launch {
            finance.authState.collect { profileRepository.selectAccount(it.uid) }
        }
    }

    fun saveTransaction(
        existing: FinanceTransaction?,
        type: String,
        amountCents: Long,
        category: FinanceCategory,
        description: String,
        occurredAt: Long
    ) = runOperation {
        finance.saveTransaction(existing, type, amountCents, category, description, occurredAt)
    }

    fun deleteTransaction(id: Long) = runOperation { finance.deleteTransaction(id) }

    fun saveCategory(name: String, type: String) = runOperation { finance.saveCategory(name, type) }

    fun deleteCategory(id: String) = runOperation { finance.deleteCategory(id) }

    fun saveBudget(category: FinanceCategory, month: String, limitCents: Long) =
        runOperation { finance.saveBudget(category, month, limitCents) }

    fun deleteBudget(categoryId: String, month: String) =
        runOperation { finance.deleteBudget(categoryId, month) }

    fun saveGoal(existing: SavingsGoal?, name: String, targetCents: Long, savedCents: Long) =
        runOperation { finance.saveGoal(existing, name, targetCents, savedCents) }

    fun deleteGoal(id: String) = runOperation { finance.deleteGoal(id) }

    fun signIn(email: String, password: String) = runAuthOperation {
        finance.signIn(email, password)
    }

    fun createAccount(email: String, password: String) = runAuthOperation {
        finance.createAccount(email, password)
    }

    fun signOut() = runAuthOperation { finance.signOut() }

    fun saveProfilePhoto(imageBytes: ByteArray) = runAuthOperation {
        profileRepository.savePhoto(imageBytes)
    }

    fun clearProfileError() = profileRepository.clearError()

    fun saveProfile(name: String, color: Int) = runAuthOperation {
        val cleanName = name.trim()
        profileRepository.save(cleanName, color)
        if (!finance.authState.value.isAnonymous) {
            finance.updateDisplayName(cleanName)
        }
    }

    private fun runAuthOperation(operation: suspend () -> Unit) = viewModelScope.launch {
        authError.value = null
        authBusy.value = true
        try {
            operation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            authError.value = error.localizedMessage ?: "Verifique seus dados e tente novamente."
        } finally {
            authBusy.value = false
        }
    }

    private fun runOperation(operation: suspend () -> Unit) = viewModelScope.launch {
        operationError.value = null
        try {
            operation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            operationError.value =
                "Não foi possível salvar os dados: ${error.localizedMessage ?: "erro local"}"
        }
    }
}

data class FinanceUiState(
    val data: FinanceData = FinanceData(),
    val syncStatus: String = "Iniciando…",
    val error: String? = null,
    val accountUid: String? = null,
    val email: String? = null,
    val isAnonymous: Boolean = true,
    val isAuthenticated: Boolean = false,
    val profileName: String = "",
    val profileColor: Int = 0,
    val profilePhotoBase64: String? = null,
    val profileError: String? = null,
    val authError: String? = null,
    val authBusy: Boolean = false
) {
    val totalBalanceCents: Long
        get() = data.transactions.sumOf { if (it.type == "INCOME") it.amountCents else -it.amountCents }

    val currentMonth: String
        get() = SimpleDateFormat("yyyy-MM", Locale.ROOT).format(Date())

    val currentMonthTransactions: List<FinanceTransaction>
        get() {
            val now = Calendar.getInstance()
            return data.transactions.filter { transaction ->
                val date = Calendar.getInstance().apply { timeInMillis = transaction.occurredAt }
                date.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            }
        }

    val monthlyIncomeCents: Long
        get() = currentMonthTransactions.filter { it.type == "INCOME" }.sumOf { it.amountCents }

    val monthlyExpenseCents: Long
        get() = currentMonthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountCents }

    val expensesByCategory: List<Pair<String, Long>>
        get() = currentMonthTransactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.categoryName }
            .mapValues { (_, rows) -> rows.sumOf { it.amountCents } }
            .toList()
            .sortedByDescending { it.second }

    fun transactionsBetween(start: Long?, end: Long?): List<FinanceTransaction> =
        data.transactions.filter { (start == null || it.occurredAt >= start) && (end == null || it.occurredAt <= end) }

    fun budgetSpent(categoryId: String, month: String): Long =
        data.transactions.filter {
            it.type == "EXPENSE" && it.categoryId == categoryId && monthFor(it.occurredAt) == month
        }.sumOf { it.amountCents }

    private fun monthFor(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM", Locale.ROOT).format(Date(timestamp))
}

private data class AccountUiState(
    val uid: String?,
    val email: String?,
    val isAnonymous: Boolean,
    val profileName: String,
    val profileColor: Int,
    val profilePhotoBase64: String?,
    val profileError: String?,
    val authError: String?,
    val authBusy: Boolean
)
