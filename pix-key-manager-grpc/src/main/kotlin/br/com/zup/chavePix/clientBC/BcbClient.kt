package br.com.zup.chavePix.clientBC

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.annotation.Client

@Client("\${app.bcbUrl}")
interface BcbClient {

    @Post
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    fun cadastraChavePix(@Body request: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>

}