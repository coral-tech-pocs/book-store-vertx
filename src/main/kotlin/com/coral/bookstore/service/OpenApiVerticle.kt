package com.coral.bookstore.service

import com.coral.bookstore.repository.LiquibaseConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.openapi.RouterBuilder

class OpenApiVerticle : AbstractVerticle(){

  override fun start(startPromise: Promise<Void>) {

    val bookHandler = BookHandler(vertx)
    val openApiService = OpenApiService(bookHandler)

    //Open Api
    val pathToContract = "/book-store.yaml"
    RouterBuilder.create(vertx, pathToContract)
      .onSuccess{
        println("HEEEEEEEEEEEEEEEERRRRRRRREEEEEEEEEEEEE")
        openApiService.buildGetBooksRoute(it)
      }
      .onFailure{
        startPromise.fail(it)
      }
  }

  override fun stop() {}
}
