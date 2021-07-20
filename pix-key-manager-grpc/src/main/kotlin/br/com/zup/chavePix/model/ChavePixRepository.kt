package br.com.zup.chavePix.model

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository: JpaRepository<ChavePix,Long> {
    fun existsByChavePix(key: String?): Boolean
    fun findByChavePix(chave: String?): Optional<ChavePix>
    fun findByIdCliente(idCliente: String?): List<ChavePix>
}