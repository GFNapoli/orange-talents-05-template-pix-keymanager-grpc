package br.com.zup.chavePix.clientBC

import java.time.LocalDateTime

data class PixKeyDetailsResponse(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
    val createdAt: LocalDateTime
)