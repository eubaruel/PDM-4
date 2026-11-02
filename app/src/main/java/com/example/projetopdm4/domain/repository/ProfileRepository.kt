package com.example.projetopdm4.domain.repository

import com.example.projetopdm4.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val profile: StateFlow<UserProfile>
    fun selectAccount(uid: String?)
    suspend fun save(name: String, color: Int)
    suspend fun savePhoto(imageBytes: ByteArray)
    fun clearError()
}
