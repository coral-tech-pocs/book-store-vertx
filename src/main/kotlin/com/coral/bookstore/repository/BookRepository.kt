package com.coral.bookstore.repository

import coral.bookstore.bookstore.entity.Book
import coral.bookstore.bookstore.models.BookInfo
import io.vertx.core.Future
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import java.util.logging.Logger


class BookRepository(private val client: PgPool) {

  //  bookName : String, page : Int, pageSize : Int
  fun list(filterMap: HashMap<String, String>): Future<List<BookInfo>> {
    val params = HashMap<String, Any>()

    var query: String = "select * from book "
    if (filterMap["title"] != "") {
      filterMap["title"] = filterMap["title"].toString()
      query = query.plus("where title =#{title} ")
    }

    val pageSize: Int = Integer.valueOf(filterMap["pageSize"].toString())
    val pageOffset: Int = (Integer.valueOf(filterMap["page"].toString()) - 1) * pageSize
    println(" pageSize : $pageSize - pageOffset : $pageOffset")

    query = query.plus("order by id offset ").plus(pageOffset)
      .plus(" fetch next ").plus(pageSize).plus(" rows only")

    return SqlTemplate
      //used * here instead of listing needed columns as data size is small
      .forQuery(client, query)
      .execute(params)
      .map { rowSet -> rowSet.asSequence().map(mapBookToDTO).toList() }
  }

  fun get(bookId: Int): Future<BookInfo> {
    val params = HashMap<String, Any>()
    params["bookId"] = bookId
    return SqlTemplate
      //used * here instead of listing needed columns as data size is small
      .forQuery(client, "select * from book where id =#{bookId}")
      .execute(params)
      .map { rowSet -> rowSet.asSequence().map(mapBookToDTO).toList()[0] }
  }

  fun insert(book: Book): Future<Long> {
    return client
      .preparedQuery("INSERT INTO book (id,isbn, title, description, price) VALUES (nextval('book_id_seq'),$1, $2, $3, $4) RETURNING (id)")
      .execute(
        Tuple.of(book.isbn, book.title, book.description, book.price)
      ).map { rowSet ->
        rowSet.iterator().next().getLong("id")
      }
  }

  fun delete(bookId: Int): Future<Int> {
    val params = HashMap<String, Any>()
    params["bookId"] = bookId

    return SqlTemplate
      .forUpdate(client, "delete from book where id=#{bookId}")
      .execute(params)
      .map { it.rowCount() }
  }

  fun update(bookId: Int, book: Book): Future<Int> {
    val params = HashMap<String, Any>()
    params["bookId"] = bookId

    params["isbn"] = book.isbn
    params["title"] = book.title
    params["description"] = book.description
    params["price"] = book.price

    return SqlTemplate
      .forUpdate(
        client,
        "update book set isbn =#{isbn}, title =#{title}, description =#{description}, price =#{price} where id =#{bookId}"
      )
      .execute(params)
      .map { it.rowCount() }
  }

  fun exists(isbn: String): Future<Boolean> {
    val params = HashMap<String, Any>()
    params["isbn"] = isbn

    return SqlTemplate
      //used * here instead of listing needed columns as data size is small
      .forQuery(client, "select * from book where isbn =#{isbn}")
      .execute(params)
      .map { rowSet ->
        rowSet.asSequence().map(mapBookToDTO).map { bookInfo -> bookInfo.isbn.equals(isbn) }.toList()[0]
      }
  }

  fun setTitle(bookId: Int, newTitle: String): Future<Int> {
    val params = HashMap<String, Any>()
    params["bookId"] = bookId
    params["title"] = newTitle
    return SqlTemplate
      .forUpdate(client, "update book set title =#{title} where id =#{bookId}")
      .execute(params)
      .map { it.rowCount() }
  }

  companion object {
    private val LOGGER = Logger.getLogger(BookRepository::class.java.name)
    val mapBookToDTO: (Row) -> BookInfo = { row ->
      BookInfo(
        row.getLong("id"),
        row.getString("isbn"),
        row.getString("title"),
        row.getLong("price")
      )
    }
  }
}
