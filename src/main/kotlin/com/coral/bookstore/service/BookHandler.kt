package com.coral.bookstore.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.coral.bookstore.repository.BookRepository
import coral.bookstore.bookstore.entity.Book
import coral.bookstore.bookstore.repository.DBConnection
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.FileUpload
import io.vertx.ext.web.RoutingContext
import java.io.File
import java.util.logging.Logger

class BookHandler(private val vertx: Vertx) {


  fun list(ctx: RoutingContext) {
    val repository = initDB(vertx)

    var bookTitle = getQueryParam(ctx, "title", "")
    var page = getQueryParam(ctx, "page", "1")
    var pageSize = getQueryParam(ctx, "pageSize", "5")

    val params = HashMap<String, String>()
    params["title"] = bookTitle
    params["page"] = page
    params["pageSize"] = pageSize

    repository.list(params)
      .onSuccess {
        ctx.response().end(Json.encodePrettily(it))
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun get(ctx: RoutingContext) {
    val repository = initDB(vertx)

    var bookId = Integer.valueOf(ctx.pathParam("id"))
    LOGGER.info("bookId : $bookId")
    repository.get(bookId)
      .onSuccess {
        ctx.response().end(Json.encodePrettily(it))
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun getImage(ctx: RoutingContext) {
    var bookId = Integer.valueOf(ctx.pathParam("id"))

    vertx.fileSystem().readFile(imagesDir.plus(bookId))
      .onSuccess{
        println(it.length())
        var response = ctx.response()
        response.putHeader("Content-Type", "application/octet")
        response.putHeader("Content-Disposition", "attachment; filename=\"picture.png\"")
        response.putHeader("Content-Length", it.length().toString())
        response.write(it)
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun insert(ctx: RoutingContext) {
    val repository = initDB(vertx)

    val formAttributes : MultiMap = ctx.request().formAttributes()
    val book = Book(0,formAttributes.get("isbn"),formAttributes.get("title"),formAttributes.get("description"),formAttributes.get("price").toLong())

    repository.insert(book)
      .onSuccess {  returnedId : Long ->
        LOGGER.info("Inserted Row Id : $returnedId")
        //upload image and rename it to be linked with book id
        val fileUploadList: List<FileUpload> = ctx.fileUploads()
        var file = fileUploadList[0]
        val lastIndexOf = file.uploadedFileName().lastIndexOf("/")
        var fullPath = file.uploadedFileName().substring(0,lastIndexOf+1).plus(returnedId)
        val src = File(file.uploadedFileName())
        val renamedTo = src.renameTo(File(fullPath))
        LOGGER.info("renamedTo: $renamedTo")

        val response = ctx.response()
        response.setChunked(true)
        response.setStatusCode(201)
        response.write("SUCCESS")
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun delete(ctx: RoutingContext) {
    val repository = initDB(vertx)
    var bookId = Integer.valueOf(ctx.pathParam("id"))
    repository.delete(bookId)
      .onSuccess {
        LOGGER.info("deleted rows : $it")
        val response = ctx.response()
        response.setChunked(true)
        response.write("SUCCESS")
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun update(ctx: RoutingContext) {
    val repository = initDB(vertx)
    var bookId = Integer.valueOf(ctx.pathParam("id"))
    val book = mapper.readValue(ctx.body().asString(), Book::class.java)

    repository.update(bookId, book)
      .onSuccess {
        LOGGER.info("updated rows : $it")
        val response = ctx.response()
        response.setChunked(true)
        response.write("SUCCESS")
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun exists(ctx: RoutingContext) {
    val repository = initDB(vertx)
    var isbn = ctx.pathParam("isbn")
    repository.exists(isbn)
      .onSuccess {
        LOGGER.info("exists : $it")
        ctx.response().end(it.toString())
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun setTitle(ctx: RoutingContext) {
    val repository = initDB(vertx)
    var bookId = Integer.valueOf(ctx.pathParam("id"))
    var newTitle = ctx.request().getParam("new_title")

    repository.setTitle(bookId, newTitle)
      .onSuccess {
        LOGGER.info("updated rows : $it")
        val response = ctx.response()
        response.setChunked(true)
        response.write("SUCCESS")
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  private fun initDB(vertx: Vertx): BookRepository {
    val dbConnection = DBConnection()
    val pgPool = dbConnection.pgPool(vertx)
    return BookRepository(pgPool)
  }

  companion object {
    private const val imagesDir = "./upload-images/"
    private val LOGGER = Logger.getLogger(BookHandler::class.java.name)
    val mapper = jacksonObjectMapper()
    val buildErrorResponse: (Throwable, RoutingContext) -> Future<Void> = { err: Throwable, ctx: RoutingContext ->
      val response = ctx.response()
      response.setChunked(true)
      response.setStatusCode(500)
      response.write("<html><body><h1>Something Error Happen</h1></body></html>")
      LOGGER.info("Error : $err")
      response.end()
    }

    private fun getQueryParam(ctx: RoutingContext, paramName: String, defaultValue: String): String {
      var value = ctx.request().getParam(paramName)
      if (value == null)
        return defaultValue
      else
        return value
    }

  }

}
