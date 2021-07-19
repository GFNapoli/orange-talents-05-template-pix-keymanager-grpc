package br.com.zup.chavePix.clientBC

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("\${app.bcbUrl}")
interface BcbClient {

    @Post
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    fun cadastraChavePix(@Body request: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>

    @Delete("/{key}")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    fun deletaChavePix(
        @PathVariable key: String,
        @Body deletePixKeyRequest: DeletePixKeyRequest
    ): HttpResponse<DeletePixKeyResponse>
}