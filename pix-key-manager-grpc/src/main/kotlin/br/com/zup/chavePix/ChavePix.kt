package br.com.zup.chavePix

import br.com.zup.TipoConta
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class ChavePix(
    val idCliente: String,
    val chavePix: String,
    val tipoChavePix: TiposChavesPix,
    val tipoConta: TipoConta
) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null
}