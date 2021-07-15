package br.com.zup.chavePix

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface ChavePixRepository: JpaRepository<ChavePix,Long> {
    fun existsByChavePix(key: String?): Boolean
}