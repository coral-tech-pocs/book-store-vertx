package com.coral.bookstore.service

import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.RouterBuilder
import java.util.*

class OpenApiService(private val bookHandler: BookHandler) {

  fun buildGetBooksRoute(routerBuilder: RouterBuilder) {
    routerBuilder
      .operation(GET_ALL_BOOKS_OPERATION)
      .handler { routingContext ->
        bookHandler.list(routingContext)
      }
  }

  companion object{
    const val GET_ALL_BOOKS_OPERATION = "getBooks"
    fun prepareResponse(routingContext: RoutingContext, code: Int, response: Any) {
      routingContext
        .response()
        .setStatusCode(code)
        .setStatusMessage("OK")
        .end(Optional.ofNullable(response).map(Json::encode).orElse("{}"))
    }
  }
}
