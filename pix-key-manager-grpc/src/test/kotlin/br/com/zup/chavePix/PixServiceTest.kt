package br.com.zup.chavePix

import br.com.zup.NovaPixKeyRequest
import br.com.zup.PixServiceGrpc
import br.com.zup.TipoChave
import br.com.zup.TipoConta
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class PixServiceTest(
    val grpcClient: PixServiceGrpc.PixServiceBlockingStub,
    val repository: ChavePixRepository
){
    @Test
    internal fun `deve retornar o id da chave pix`() {
        repository.deleteAll()
        val response = grpcClient.novaChavePix(NovaPixKeyRequest.newBuilder()
            .setIdCliente("5260263c-a3c1-4727-ae32-3bdb2538841b")
            .setKey("86135457004")
            .setTipoChave(TipoChave.CPF)
            .setTipoConta(TipoConta.CORRENTE)
            .build())

        with(response){
            assertNotNull(response.pixId)
            assertTrue(repository.existsById(response.pixId))
        }
    }

    @Test
    internal fun `deve retornar erro chave ja existente`() {
        repository.deleteAll()

        val teste = ChavePix("de95a228-1f27-4ad2-907e-e5a2d816e9bc", "31643468081",
            TiposChavesPix.CPF, TipoConta.CORRENTE)
        repository.save(teste)
        val response = assertThrows<StatusRuntimeException>{
            grpcClient.novaChavePix(NovaPixKeyRequest.newBuilder()
            .setIdCliente(teste.idCliente)
            .setKey(teste.chavePix)
            .setTipoChave(TipoChave.CPF)
            .setTipoConta(teste.tipoConta)
            .build())
        }

        with(response){
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals(status.description, "Chave já cadastrada")
        }
    }

    @Test
    internal fun `deve retornar erro tipo de chave invalida`() {
        repository.deleteAll()
        val response = assertThrows<StatusRuntimeException>{
            grpcClient.novaChavePix(NovaPixKeyRequest.newBuilder()
                .setIdCliente("5260263c-a3c1-4727-ae32-3bdb2538841b")
                .setKey("861354sd5700415965482a456ss")
                .setTipoChave(TipoChave.CPF)
                .setTipoConta(TipoConta.CORRENTE)
                .build())
        }
        with(response){
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals(status.description, "Tipo de chave não condiz com chave recebida ou chave invalida")
        }

    }

    @Factory
    class client{
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PixServiceGrpc.PixServiceBlockingStub{
            return PixServiceGrpc.newBlockingStub(channel)
        }
    }
}