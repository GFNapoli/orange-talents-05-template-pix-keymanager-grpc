package br.com.zup.chavePix

import br.com.zup.*
import br.com.zup.chavePix.clientBC.*
import br.com.zup.chavePix.clientErpItau.ErpClient
import br.com.zup.chavePix.model.ChavePix
import br.com.zup.chavePix.model.ChavePixRepository
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpResponse
import org.jetbrains.annotations.NotNull
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PixService(
    @Inject val repository: ChavePixRepository,
    @Inject val erpClient: ErpClient,
    @Inject val bcbClient: BcbClient
): PixServiceGrpc.PixServiceImplBase() {

    override fun novaChavePix(request: NovaPixKeyRequest?, responseObserver: StreamObserver<NovaPixKeyResponse>?) {

        if(repository.existsByChavePix(request!!.key)){
            responseObserver?.onError(Status.ALREADY_EXISTS
                .withDescription("Chave já cadastrada")
                .asRuntimeException())
            return
        }
        val tipoChave = TiposChavesPix.validaChave(request.key)
        if(tipoChave!! != TiposChavesPix.fromValue(request.tipoChave.toString().toLowerCase())
            || TiposChavesPix.fromValue(request.tipoChave.toString().toLowerCase()) == TiposChavesPix.INVALIDA){
            responseObserver?.onError(Status.INVALID_ARGUMENT
                .withDescription("Tipo de chave não condiz com chave recebida ou chave invalida")
                .asRuntimeException())
            return
        }

        val dadosErp = erpClient.consultaConta(request.idCliente, request.tipoConta.toString())
        if(dadosErp.body() == null){
            responseObserver?.onError(Status.ABORTED
                .withDescription("Erro ao consultar sistema ERP, ou cliente não cadatrado")
                .asRuntimeException())
            return
        }

        val tipoContaBcb: String = if (dadosErp.body()!!.tipo == "CONTA_CORRENTE") "CACC" else "SVGS"

        val bankAccount = BankAccount(
            dadosErp.body()!!.instituicao.ispb,
            dadosErp.body()!!.agencia,
            dadosErp.body()!!.numero,
            tipoContaBcb
        )
        val owner = Owner(
            "NATURAL_PERSON",
            dadosErp.body()!!.titular.nome,
            dadosErp.body()!!.titular.cpf
        )

        var pixKeyResponse: HttpResponse<CreatePixKeyResponse>? = null

        if(tipoChave == TiposChavesPix.RANDOM){
            val pixRequest = CreatePixKeyRequest(
                tipoChave.toString(),
                "",
                bankAccount,
                owner
            )
            pixKeyResponse = bcbClient.cadastraChavePix(pixRequest)
        }else{
            if (tipoChave == TiposChavesPix.CPF){
                if (request.key != dadosErp.body()!!.titular.cpf){
                    responseObserver?.onError(Status.INVALID_ARGUMENT
                        .withDescription("O CPF usado como chave não pertence ao solicitante")
                        .asRuntimeException())
                    return
                }
            }
            val pixRequest = CreatePixKeyRequest(
                tipoChave.toString(),
                request.key,
                bankAccount,
                owner
            )
            pixKeyResponse = bcbClient.cadastraChavePix(pixRequest)
        }

        if(pixKeyResponse.body() == null){
            responseObserver?.onError(Status.ABORTED
                .withDescription("Erro durante a comunicação com o Banco Central")
                .asRuntimeException())
            return
        }

        val chavePix = ChavePix(
            dadosErp.body()!!.titular.id,
            pixKeyResponse.body()!!.key,
            tipoChave,
            request.tipoConta,
            pixKeyResponse.body()!!.createdAt
        )

        repository.save(chavePix)

        responseObserver?.onNext(NovaPixKeyResponse.newBuilder().setPixId(chavePix.id!!).build())
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

        if(chave.get().idCliente != request!!.idCliente){
            responseObserver?.onError(Status.INVALID_ARGUMENT
                .withDescription("Apenas o dono da chave pode solicitar a deleção")
                .asRuntimeException())
            return
        }

        val deletePixKeyRequest = DeletePixKeyRequest(chave.get().chavePix)
        val deletaKeyResponse = bcbClient.deletaChavePix(chave.get().chavePix, deletePixKeyRequest)

        if(deletaKeyResponse.body() == null){
            responseObserver?.onError(Status.ABORTED
                .withDescription("Erro ao comunicar com o bamco central, tente novamente mais tarde")
                .asRuntimeException())
            return
        }

        repository.deleteById(chave.get().id)
        responseObserver?.onNext(DeletaKeyResponse.newBuilder()
            .setMensagem("Exclusao concluida com sucesso")
            .build())
        responseObserver?.onCompleted()

    }

    override fun consultaChavePix(request: ConsultaKeyRequest?, responseObserver: StreamObserver<ConsultaKeyResponse>?) {

        if(request!!.hasField(ConsultaKeyRequest.getDescriptor().findFieldByName("pixId"))){
            val chavePix = repository.findById(request.pixId)

            if(chavePix.isEmpty){
                responseObserver?.onError(Status.NOT_FOUND
                    .withDescription("PiXID não consta no sistema")
                    .asRuntimeException())
                return
            }

            if (chavePix.get().idCliente != request!!.idCliente){
                responseObserver?.onError(Status.INVALID_ARGUMENT
                    .withDescription("O idCliente invalido para PixId informado")
                    .asRuntimeException())
                return
            }

            val chaveConsultada = bcbClient.consultaChavePix(chavePix.get().chavePix)

            if (chaveConsultada.body() == null){
                responseObserver?.onError(Status.FAILED_PRECONDITION
                    .withDescription("Chave não consta no banco central, chave ainda invalida")
                    .asRuntimeException())
                return
            }

            val chaveResponse = chaveConsultada.body()
            val tipoConta = if(chaveResponse!!.bankAccount.accountType == "CACC") TipoConta.CONTA_CORRENTE else TipoConta.CONTA_POUPANCA

            responseObserver?.onNext(ConsultaKeyResponse.newBuilder()
                .setTipoChave(chaveResponse.keyType)
                .setKey(chaveResponse.key)
                .setNome(chaveResponse.owner.name)
                .setCpf(chaveResponse.owner.taxIdNumber)
                .setDadosConta(ConsultaKeyResponse.DadosConta.newBuilder()
                    .setInstituicao(chaveResponse.bankAccount.participant)
                    .setAgencia(chaveResponse.bankAccount.branch)
                    .setNumero(chaveResponse.bankAccount.accountNumber)
                    .setTipoConta(tipoConta).build())
                .setDatacriacao(Timestamp.newBuilder().setNanos(chaveResponse.createdAt.nano)
                    .setSeconds(chaveResponse.createdAt.toEpochSecond(ZoneOffset.UTC)))
                .setPixId(chavePix.get().id!!)
                .setIdCliente(chavePix.get().idCliente)
                .build())
            responseObserver?.onCompleted()
        }

        val chaveConsultada = bcbClient.consultaChavePix(request.chave)
        if (chaveConsultada.body() == null){
            responseObserver?.onError(Status.INVALID_ARGUMENT
                .withDescription("Chave não existente")
                .asRuntimeException())
            return
        }
        val chaveResponse = chaveConsultada.body()
        val tipoConta = if(chaveResponse!!.bankAccount.accountType == "CACC") TipoConta.CONTA_CORRENTE else TipoConta.CONTA_POUPANCA

        responseObserver?.onNext(ConsultaKeyResponse.newBuilder()
            .setTipoChave(chaveResponse.keyType)
            .setKey(chaveResponse.key)
            .setNome(chaveResponse.owner.name)
            .setCpf(chaveResponse.owner.taxIdNumber)
            .setDadosConta(ConsultaKeyResponse.DadosConta.newBuilder()
                .setInstituicao(chaveResponse.bankAccount.participant)
                .setAgencia(chaveResponse.bankAccount.branch)
                .setNumero(chaveResponse.bankAccount.accountNumber)
                .setTipoConta(tipoConta).build())
            .setDatacriacao(Timestamp.newBuilder().setNanos(chaveResponse.createdAt.nano)
                .setSeconds(chaveResponse.createdAt.toEpochSecond(ZoneOffset.UTC)))
            .build())
        responseObserver?.onCompleted()
    }
}