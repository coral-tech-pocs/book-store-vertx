package com.coral.bookstore.service

import com.coral.bookstore.repository.BookRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import coral.bookstore.bookstore.entity.Book
import coral.bookstore.bookstore.models.BookInfo
import coral.bookstore.bookstore.repository.DBConnection
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.FileUpload
import io.vertx.ext.web.RoutingContext
import java.io.File
import java.util.logging.Level
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
    val bookId: Int
    try {
      bookId = Integer.valueOf(ctx.pathParam("id"))
    } catch (e: NumberFormatException) {
      ctx.fail(400, e)
      return
    }
    LOGGER.info("bookId : $bookId")
    repository.get(bookId)
      .onSuccess {
        ctx.response().end(Json.encodePrettily(it))
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun getImage(ctx: RoutingContext) {
    val bookId: Int
    try {
      bookId = Integer.valueOf(ctx.pathParam("id"))
    } catch (e: NumberFormatException) {
      ctx.fail(400, e)
      return
    }
    vertx.fileSystem().readFile(imagesDir.plus(bookId))
      .onSuccess {
        println(it.length())
        var response = ctx.response()
        response.putHeader("Content-Type", "application/octet")
        response.putHeader("Content-Disposition", "attachment; filename=\"picture.png\"")
        response.putHeader("Content-Length", it.length().toString())
        response.write(it)
        response.end()
      }
      .onFailure { err -> buildErrorResponse(err, ctx) }
  }

  fun insert(ctx: RoutingContext) {
    val repository = initDB(vertx)

    val book : Book
    try {
      val formAttributes: MultiMap = ctx.request().formAttributes()
      if (formAttributes.get("isbn").isNullOrEmpty() || formAttributes.get("title").isNullOrEmpty()
        || formAttributes.get("price").isNullOrEmpty() || formAttributes.get("price").toLong() < 0) {
        ctx.fail(400, Exception("Expected form attributes"))
        return
      } else
        book = Book(
        0,
        formAttributes.get("isbn"),
        formAttributes.get("title"),
        formAttributes.get("description"),
        formAttributes.get("price").toLong())

    } catch (e: Exception) {
      println("println - Validation error : $e")
      LOGGER.log(Level.SEVERE,"Validation error : $e")
      ctx.fail(400, e)
      return
    }

    repository.insert(book)
      .onSuccess { returnedId: Long ->
        LOGGER.info("Inserted Row Id : $returnedId")
        //upload image and rename it to be linked with book id
        if(!ctx.fileUploads().isNullOrEmpty()){
          val fileUploadList: List<FileUpload> = ctx.fileUploads()
          var file = fileUploadList[0]
          val lastIndexOf = file.uploadedFileName().lastIndexOf("/")
          var fullPath = file.uploadedFileName().substring(0, lastIndexOf + 1).plus(returnedId)
          val src = File(file.uploadedFileName())
          val renamedTo = src.renameTo(File(fullPath))
          LOGGER.info("renamedTo: $renamedTo")
        }

        val response = ctx.response()
        response.isChunked = true
        response.statusCode = 201
        response.write(returnedId.toString())
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun delete(ctx: RoutingContext) {
    val repository = initDB(vertx)
    val bookId: Int
    try {
      bookId = Integer.valueOf(ctx.pathParam("id"))
    } catch (e: NumberFormatException) {
      ctx.fail(400, e)
      return
    }
    repository.delete(bookId)
      .onSuccess {
        LOGGER.info("deleted rows : $it")
        val response = ctx.response()
        response.setChunked(true)
        response.write(it.toString())
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun update(ctx: RoutingContext) {
    val repository = initDB(vertx)
    val bookId : Int
    val book : Book
    try {
      bookId = Integer.valueOf(ctx.pathParam("id"))
      book = mapper.readValue(ctx.body().asString(), Book::class.java)
      if(notValidBook(book)) {
        ctx.fail(400)
        return
      }
    } catch (e: Exception) {
      ctx.fail(400, e)
      return
    }

    repository.update(bookId, book)
      .onSuccess {
        LOGGER.info("updated rows : $it")
        val response = ctx.response()
        response.setChunked(true)
        response.write(it.toString())
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun exists(ctx: RoutingContext) {
    val repository = initDB(vertx)
    val isbn : String
    try {
      isbn = ctx.pathParam("isbn")
    } catch (e: Exception) {
      ctx.fail(400, e)
      return
    }
    repository.exists(isbn)
      .onSuccess {
        var response = ctx.response()
        LOGGER.info("exists : ${it.isNotEmpty()}")

        if(it.isNotEmpty())
          response.putHeader("isExist", "true")
        else
          response.putHeader("isExist", "false")
        response.end()
      }
      .onFailure { buildErrorResponse(it, ctx) }
  }

  fun setTitle(ctx: RoutingContext) {
    val repository = initDB(vertx)
    val bookId : Int
    val newTitle : String
    try {
      bookId = Integer.valueOf(ctx.pathParam("id"))
      newTitle = ctx.request().getParam("new_title")
      if(newTitle.isNullOrEmpty()){
        ctx.fail(400)
        return
      }
    } catch (e: Exception) {
      ctx.fail(400, e)
      return
    }

    repository.setTitle(bookId, newTitle)
      .onSuccess {
        LOGGER.info("updated rows : $it")
        ctx.response().end(it.toString())
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
      ctx.fail(500, Exception("Something Error Happen"))
      LOGGER.log(Level.SEVERE,"buildErrorResponse - Error : $err")
      Promise.promise<Void>().future()
    }

    private fun getQueryParam(ctx: RoutingContext, paramName: String, defaultValue: String): String {
      return ctx.request().getParam(paramName) ?: defaultValue
    }

    private fun notValidBook(book : Book) : Boolean {
      return (book.isbn.isNullOrEmpty() || book.title.isNullOrEmpty()
        || book.price < 0)
    }
  }

}
