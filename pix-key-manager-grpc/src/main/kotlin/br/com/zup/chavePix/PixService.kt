package br.com.zup.chavePix

import br.com.zup.*
import br.com.zup.chavePix.clientErpItau.ErpClient
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PixService(
    @Inject val repository: ChavePixRepository,
    @Inject val erpClient: ErpClient
): PixServiceGrpc.PixServiceImplBase() {

    override fun novaChavePix(request: NovaPixKeyRequest?, responseObserver: StreamObserver<NovaPixKeyResponse>?) {

        if(repository.existsByChavePix(request!!.key)){
            responseObserver?.onError(Status.ALREADY_EXISTS
                .withDescription("Chave já cadastrada")
                .asRuntimeException())
            responseObserver?.onCompleted()
        }
        val tipoChave = TiposChavesPix.validaChave(request.key)
        if(tipoChave!! != TiposChavesPix.fromValue(request.tipoChave.toString().toLowerCase())
            || TiposChavesPix.fromValue(request.tipoChave.toString().toLowerCase()) == TiposChavesPix.INVALIDA){
            responseObserver?.onError(Status.INVALID_ARGUMENT
                .withDescription("Tipo de chave não condiz com chave recebida ou chave invalida")
                .asRuntimeException())
            responseObserver?.onCompleted()
        }

        val dadosErp = erpClient.consultaConta(request.idCliente, request.tipoConta.toString())
        if(dadosErp .body() == null){
            responseObserver?.onError(Status.ABORTED
                .withDescription("erro ao consultar sistema ERP")
                .asRuntimeException())
            responseObserver?.onCompleted()
        }
        
        if(tipoChave == TiposChavesPix.RANDOM){
            val key = UUID.randomUUID().toString()
            val chavePix = ChavePix(request.idCliente, key, tipoChave, request.tipoConta)
            repository.save(chavePix)
            responseObserver?.onNext(NovaPixKeyResponse.newBuilder().setPixId(chavePix.id!!).build())
        }else{
            val chavePix = ChavePix(request.idCliente, request.key, tipoChave, request.tipoConta)
            repository.save(chavePix)
            responseObserver?.onNext(NovaPixKeyResponse.newBuilder().setPixId(chavePix.id!!).build())
        }
        responseObserver?.onCompleted()
    }

    override fun deletaChavePix(request: DeletaKeyRequest?, responseObserver: StreamObserver<DeletaKeyResponse>?) {

        val chave = repository.findById(request!!.pixId)

        if (chave.isEmpty){
            responseObserver?.onError(Status.NOT_FOUND
                .withDescription("Chave não encontrada")
                .asRuntimeException())
            return
        }

        println(chave.get().idCliente != request!!.idCliente)
        if(chave.get().idCliente != request!!.idCliente){
            responseObserver?.onError(Status.INVALID_ARGUMENT
                .withDescription("Apenas o dono da chave pode solicitar a deleção")
                .asRuntimeException())
            return
        }

        repository.deleteById(chave.get().id)
        responseObserver?.onNext(DeletaKeyResponse.newBuilder()
            .setMensagem("Exclusao concluida com sucesso")
            .build())
        responseObserver?.onCompleted()

    }
}