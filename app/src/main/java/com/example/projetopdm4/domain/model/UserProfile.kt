package com.example.projetopdm4.domain.model

data class UserProfile(
    val uid: String?,
    val name: String = "",
    val color: Int = 0,
    val photoBase64: String? = null,
    val error: String? = null
)
