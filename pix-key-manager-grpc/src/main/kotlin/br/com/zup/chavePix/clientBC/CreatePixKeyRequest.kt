package br.com.zup.chavePix.clientBC


data class CreatePixKeyRequest(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner
) {
}