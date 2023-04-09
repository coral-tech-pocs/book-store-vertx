package coral.bookstore.bookstore.repository

import coral.bookstore.bookstore.entity.Author
import io.vertx.core.Future
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.templates.SqlTemplate

class AuthorRepository(private val client: PgPool) {

  fun findAll(): Future<List<Author>> {
    return  SqlTemplate
      //used * here instead of listing needed columns as data size is small
      .forQuery(client, "SELECT * FROM author")
      .execute(emptyMap()) // no param to filter with
      .map { rowSet -> rowSet.asSequence().map(mapFun).toList() }
  }

  companion object {
    // here we defined mapFun of type [function type]
    val mapFun: (Row) -> Author = { row ->
      Author(
        row.getLong("id"),
        row.getString("name")
      )
    }
  }
}
