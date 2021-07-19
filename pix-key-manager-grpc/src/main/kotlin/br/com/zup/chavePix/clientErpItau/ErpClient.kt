package br.com.zup.chavePix.clientErpItau

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client


@Client("\${app.erpUrl}")
interface ErpClient{

    @Get("{idCliente}/contas")
    fun consultaConta(@PathVariable idCliente: String, @QueryValue tipo: String): HttpResponse<ContaResponse>

}