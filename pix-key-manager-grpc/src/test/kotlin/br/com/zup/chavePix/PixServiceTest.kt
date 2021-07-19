package br.com.zup.chavePix

import br.com.zup.*
import br.com.zup.chavePix.clientBC.*
import br.com.zup.chavePix.model.ChavePix
import br.com.zup.chavePix.model.ChavePixRepository
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.internal.configuration.ClassPathLoader
import java.time.LocalDateTime
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class PixServiceTest(
    val grpcClient: PixServiceGrpc.PixServiceBlockingStub,
    val repository: ChavePixRepository,
    val bcbClient: BcbClient
){
    @Test
    internal fun `deve retornar o id da chave pix`() {
        repository.deleteAll()

        val bankAccount = BankAccount("60701190", "0001", "291900", "CACC")
        val owner = Owner("NATURAL_PERSON", "Rafael M C Ponte", "02467781054")
        val mockRequest = CreatePixKeyRequest(TiposChavesPix.CPF.toString(), "02467781054", bankAccount, owner)
        val mockResponse = CreatePixKeyResponse(TiposChavesPix.CPF.toString(),"02467781054", bankAccount, owner, LocalDateTime.now())
        Mockito.`when`(bcbClient.cadastraChavePix(mockRequest)).thenReturn(HttpResponse.ok(mockResponse))

        val response = grpcClient.novaChavePix(NovaPixKeyRequest.newBuilder()
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setKey("02467781054")
            .setTipoChave(TipoChave.CPF)
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build())

        with(response){
            assertNotNull(response.pixId)
            assertTrue(repository.existsById(response.pixId))
        }
    }

    @Test
    internal fun `deve criar uma chave aleatoria`() {
        repository.deleteAll()
        val bankAccount = BankAccount("60701190", "0001", "291900", "CACC")
        val owner = Owner("NATURAL_PERSON", "Rafael M C Ponte", "02467781054")
        val mockRequest = CreatePixKeyRequest(TiposChavesPix.RANDOM.toString(), "", bankAccount, owner)
        val mockResponse = CreatePixKeyResponse(TiposChavesPix.RANDOM.toString(),"12a6s4d65a4s56das2d1", bankAccount, owner, LocalDateTime.now())
        Mockito.`when`(bcbClient.cadastraChavePix(mockRequest)).thenReturn(HttpResponse.ok(mockResponse))
        val response = grpcClient.novaChavePix(NovaPixKeyRequest.newBuilder()
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setKey("")
            .setTipoChave(TipoChave.ALEATORIA)
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build())

        with(response){
            assertNotNull(response.pixId)
            assertTrue(repository.existsById(response.pixId))
        }
    }

    @Test
    internal fun `deve retornar erro chave ja existente`() {
        repository.deleteAll()

        val teste = ChavePix("c56dfef4-7901-44fb-84e2-a2cefb157890", "02467781054",
            TiposChavesPix.CPF, TipoConta.CONTA_CORRENTE, LocalDateTime.now())

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
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }
        with(response){
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals(status.description, "Tipo de chave não condiz com chave recebida ou chave invalida")
        }
    }

    @Test
    internal fun `deve deletar a chave pix`() {
        repository.deleteAll()

        val teste = ChavePix("ae93a61c-0642-43b3-bb8e-a17072295955", "40764442058",
            TiposChavesPix.CPF, TipoConta.CONTA_CORRENTE, LocalDateTime.now())

        repository.save(teste)

        val delRequest = DeletePixKeyRequest("40764442058")
        val delResponse = DeletePixKeyResponse("40764442058", "60701190", LocalDateTime.now())

        Mockito.`when`(bcbClient.deletaChavePix("40764442058", delRequest)).thenReturn(HttpResponse.ok(delResponse))

        val response = grpcClient.deletaChavePix(DeletaKeyRequest.newBuilder()
            .setPixId(teste.id!!)
            .setIdCliente(teste.idCliente)
            .build())

        with(response){
            assertNotNull(response.mensagem)
            assertTrue(!repository.existsById(teste.id))
        }
    }

    @Test
    internal fun `delecao de chave nao existente`() {
        repository.deleteAll()

        val response = assertThrows<StatusRuntimeException>{
            grpcClient.deletaChavePix(DeletaKeyRequest.newBuilder()
                .setPixId(12)
                .setIdCliente("de95a228-1f27-4ad2-907e-e5a2d816e9bc")
                .build())
        }

        with(response){
            assertEquals(status.code, Status.NOT_FOUND.code)
            assertEquals(status.description, "Chave não encontrada")
        }
    }

    @Test
    internal fun `a delecao deve ser apenas por quem cadastrou`() {
        repository.deleteAll()

        val teste = ChavePix("ae93a61c-0642-43b3-bb8e-a17072295955", "40764442058",
            TiposChavesPix.CPF, TipoConta.CONTA_CORRENTE, LocalDateTime.now())

        repository.save(teste)

        val response = assertThrows<StatusRuntimeException>{
            grpcClient.deletaChavePix(DeletaKeyRequest.newBuilder()
                .setPixId(teste.id!!)
                .setIdCliente("de95a228-1f27-4ad2-907e-e5a2d816e9bc")
                .build())
        }

        with(response){
            assertEquals(status.code, Status.INVALID_ARGUMENT.code)
            assertEquals(status.description, "Apenas o dono da chave pode solicitar a deleção")
        }
    }

    @MockBean(BcbClient::class)
    fun bcbMock(): BcbClient{
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class client{
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PixServiceGrpc.PixServiceBlockingStub{
            return PixServiceGrpc.newBlockingStub(channel)
        }
    }
}