package com.example.projetopdm4.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.projetopdm4.data.local.FinanceDatabase
import com.example.projetopdm4.data.repository.FinanceRepositoryImpl
import com.example.projetopdm4.data.repository.FirebaseProfileRepository
import com.example.projetopdm4.domain.usecase.FinanceUseCases

class FinanceViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FinanceViewModel::class.java))
        val database = FinanceDatabase.get(appContext)
        val repository = FinanceRepositoryImpl(database.financeDao(), appContext)
        val profileRepository = FirebaseProfileRepository(appContext)
        return FinanceViewModel(FinanceUseCases(repository), profileRepository) as T
    }
}
