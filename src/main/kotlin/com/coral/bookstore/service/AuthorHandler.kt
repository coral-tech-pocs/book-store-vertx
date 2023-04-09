package coral.bookstore.bookstore.service

import coral.bookstore.bookstore.entity.Author
import coral.bookstore.bookstore.models.BookInfo
import coral.bookstore.bookstore.repository.AuthorRepository
import coral.bookstore.bookstore.repository.DBConnection
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext

class AuthorHandler (private val vertx : Vertx) {

  fun getAuthorBooks(ctx : RoutingContext) {
    val repository = initDB(vertx)
    repository.findAll()
      .onSuccess{
      ctx.response().end(Json.encodePrettily(it))
      }
      .onFailure{
          err -> ctx.response().end("<h1>No Data Found</h1> \n $err")
      }
  }

  private fun initDB(vertx : Vertx) : AuthorRepository {
    val dbConnection = DBConnection()
    val pgPool = dbConnection.pgPool(vertx)
    return AuthorRepository(pgPool)
  }
}
