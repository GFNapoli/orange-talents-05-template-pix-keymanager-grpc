package br.com.zup.chavePix.clientBC

data class BankAccount(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: String
)
