package br.com.zup.chavePix.clientBC

import java.time.LocalDateTime

data class DeletePixKeyResponse(val key: String, val participant: String, val deletedAt: LocalDateTime)
