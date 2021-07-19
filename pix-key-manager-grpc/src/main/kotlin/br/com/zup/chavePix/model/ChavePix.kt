package br.com.zup.chavePix.model

import br.com.zup.TipoConta
import br.com.zup.chavePix.TiposChavesPix
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class ChavePix(
    val idCliente: String,
    val chavePix: String,
    val tipoChavePix: TiposChavesPix,
    val tipoConta: TipoConta,
    val criadoEm: LocalDateTime
) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null
}