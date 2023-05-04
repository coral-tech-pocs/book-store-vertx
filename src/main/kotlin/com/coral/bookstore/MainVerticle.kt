package com.coral.bookstore

import com.coral.bookstore.repository.LiquibaseConfig
import com.coral.bookstore.service.BookHandler
import com.coral.bookstore.service.OpenApiService
import com.coral.bookstore.service.OpenApiVerticle
import coral.bookstore.bookstore.service.AuthorHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.ext.web.validation.ValidationHandler
import java.util.logging.Level
import java.util.logging.Logger


class MainVerticle : AbstractVerticle() {

  override fun start(startPromise: Promise<Void>) {

    val bookHandler = BookHandler(vertx)
//    val authorHandler = AuthorHandler(vertx)

    //liquibase code
//    vertx.executeBlocking { promise: Promise<Any> -> LiquibaseConfig.runLiquibaseScripts(vertx, promise)}

    vertx.deployVerticle(OpenApiVerticle())

    vertx.createHttpServer()
      .requestHandler(bookRoutes(bookHandler))
//      .requestHandler(authorRoutes(authorHandler))
      .listen(8090)
      .onSuccess {
        startPromise.complete()
        println("HTTP server started on port " + it.actualPort())
      }
      .onFailure {
        startPromise.fail(it)
        println("Failed to start HTTP server:" + it.message)
      }

  }

  private fun bookRoutes(bookHandler: BookHandler): Router {
    var router = Router.router(vertx)
    router.route().failureHandler(this::handleFailure)

    router.get("/books").handler { ctx -> bookHandler.list(ctx) }
    router.get("/books/:id").handler { ctx -> bookHandler.get(ctx) }
    router.get("/books/:id/image").handler { ctx -> bookHandler.getImage(ctx) }
    router.post("/books")
      .handler(
        BodyHandler.create().setHandleFileUploads(true).setUploadsDirectory("./upload-images")
          .setMergeFormAttributes(true)
      )
      .handler { ctx -> bookHandler.insert(ctx) }
    router.delete("/books/:id").handler { ctx -> bookHandler.delete(ctx) }
    router.put("/books/:id").handler(BodyHandler.create()).handler { ctx -> bookHandler.update(ctx) }
    router.head("/books/:isbn").handler { ctx -> bookHandler.exists(ctx) }
    router.patch("/books/:id").handler { ctx -> bookHandler.setTitle(ctx) }
    return router
  }

  private fun authorRoutes(author: AuthorHandler): Router {
    var router = Router.router(vertx)
    router.get("/authors").handler { ctx -> author.getAuthorBooks(ctx) }
    return router
  }

  private fun handleFailure(routingContext: RoutingContext) {
    val logger = Logger.getLogger(MainVerticle::class.java.name)
    logger.log(Level.SEVERE, "handleFailure - Error : " + routingContext.failure()?.stackTraceToString())
    val res = routingContext.response()
    res.putHeader("Content-type", "application/json; charset=utf-8")
    res.statusCode = routingContext.statusCode()
    res.isChunked = true
    res.write("" + routingContext.failure()?.message)
    res.end()
  }

  // Optional - called when verticle is undeployed
  override fun stop() {}

}
