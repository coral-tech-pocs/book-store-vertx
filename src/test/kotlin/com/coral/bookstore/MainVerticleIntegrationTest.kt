package com.coral.bookstore

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import coral.bookstore.bookstore.entity.Book
import coral.bookstore.bookstore.models.BookInfo
import io.reactiverse.junit5.web.TestRequest.*
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.get
import io.vertx.uritemplate.UriTemplate
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.logging.Logger
import kotlin.random.Random


@ExtendWith(VertxExtension::class)
class MainVerticleIntegrationTest {

  val logger = Logger.getLogger(MainVerticleIntegrationTest::class.java.name)

  @BeforeEach
  fun deploy_verticle(vertx: Vertx, testContext: VertxTestContext) {
    vertx.deployVerticle(MainVerticle(), testContext.succeeding<String> { _ -> testContext.completeNow() })
  }

  @Test
  @DisplayName("test_return_all_books")
  fun test_return_all_books(vertx: Vertx, testContext: VertxTestContext) {

    var bookInfoToBeMatched = BookInfo(1, "zxc3", "Test 1", 52)
    var client: HttpClient = vertx.createHttpClient()
    client.request(HttpMethod.GET, 8090, "localhost", "/books")
      .compose(HttpClientRequest::send)
      .compose(HttpClientResponse::body)
      .onSuccess { body ->
        testContext.verify {
          val bookInfos = toList(body.toJsonArray())
          assertThat(bookInfos).anyMatch { it -> checkingBookMatching(it, bookInfoToBeMatched) }
        }
        testContext.completeNow()
      }
      .onFailure { failure -> testContext.failNow(failure) };
  }

  @Test
  @DisplayName("test_return_book_by_id")
  fun test_return_book_by_id(vertx: Vertx, testContext: VertxTestContext) {

    var bookInfoToBeMatched = BookInfo(4, "zxc6", "Test 4", 52)
    var client: HttpClient = vertx.createHttpClient()
    client.request(HttpMethod.GET, 8090, "localhost", "/books/4")
      .compose(HttpClientRequest::send)
      .compose(HttpClientResponse::body)
      .onSuccess { body ->
        testContext.verify {
          var bookInfoResult = jacksonObjectMapper().readValue(body.toJsonObject().toString(), BookInfo::class.java)
          assertThat(bookInfoResult).isEqualTo(bookInfoToBeMatched)
        }
        testContext.completeNow()
      }
      .onFailure { failure -> testContext.failNow(failure) };
  }

  @Test
  @DisplayName("test_return_book_image_by_id")
  fun test_return_book_image_by_id(vertx: Vertx, testContext: VertxTestContext) {

    val imageBuffer: Buffer = vertx.fileSystem().readFileBlocking("./upload-images/34")
    var client: HttpClient = vertx.createHttpClient()
    client.request(HttpMethod.GET, 8090, "localhost", "/books/34/image")
      .compose(HttpClientRequest::send)
      .onSuccess { body ->
        testContext.verify {
          assertThat(body.getHeader("Content-Length")).isEqualTo(imageBuffer.length().toString())
        }
        testContext.completeNow()
      }
      .onFailure { failure -> testContext.failNow(failure) };
  }

  @Test
  @DisplayName("test_return_book_image_by_id_throw_exception")
  fun test_return_book_image_by_id_throw_exception(vertx: Vertx, testContext: VertxTestContext) {

    var client: HttpClient = vertx.createHttpClient()
    client.request(HttpMethod.GET, 8090, "localhost", "/books/0/image")
      .compose(HttpClientRequest::send)
      .compose(HttpClientResponse::body)
      .onSuccess {
        testContext.verify {
          assertThat(it.toString()).isEqualTo("Something Error Happen")
        }
        testContext.completeNow()
      }
      .onFailure { failure -> testContext.failNow(failure) };
  }

  @Test
  @DisplayName("test_add_book")
  fun test_add_book(vertx: Vertx, testContext: VertxTestContext) {

    val counter: Int = Random.nextInt(0, 200)
    val form: MultiMap = MultiMap.caseInsensitiveMultiMap()
    val isbn: String = "Test".plus(counter)
    form.set("isbn", isbn);
    val title: String = "Test".plus(counter)
    logger.info("add-book title : $title")
    form.set("title", title);
    form.set("description", "any test to describe my book");
    form.set("price", "64");

    var bookInfoToBeMatched = BookInfo(0, isbn, title, 64)
    val webClient = WebClient.create(vertx)
    webClient.post(8090, "localhost", "/books")
      .putHeader("content-type", "multipart/form-data")
      .sendForm(form)
      .onFailure { failure -> testContext.failNow(failure) }
      .onSuccess {
        logger.info("add-Book-res : ${it.body().toString()}")

        testContext.verify {
          assertThat(it.body().toString()).isAlphanumeric

          val client2 = vertx.createHttpClient()
          client2.request(HttpMethod.GET, 8090, "localhost", "/books?title=".plus(title))
            .compose(HttpClientRequest::send)
            .compose(HttpClientResponse::body)
            .onFailure { failure -> testContext.failNow(failure) }
            .onSuccess { body ->
              testContext.verify {
                val bookInfos = toList(body.toJsonArray())
                logger.info("bookInfos : $bookInfos")
                assertThat(bookInfos).anyMatch { bookInfo -> checkingBookMatching(bookInfo, bookInfoToBeMatched) }
                testContext.completeNow()
              }
            }
        }
      }
  }

  @Test
  @DisplayName("test_delete_book")
  fun test_delete_book(vertx: Vertx, testContext: VertxTestContext) {

    val webClient = WebClient.create(vertx)
    val deleteRequestURI: UriTemplate = UriTemplate.of("/books/{book_Id}")

    val counter: Int = Random.nextInt(0, 200)
    val form: MultiMap = MultiMap.caseInsensitiveMultiMap()
    form.set("isbn", "zxc".plus(counter));
    val title: String = "Test".plus(counter)
    form.set("title", title);
    form.set("description", "any test to describe my book");
    form.set("price", "64");

    val deleteWebClient = WebClient.create(vertx)
    webClient.post(8090, "localhost", "/books")
      .putHeader("content-type", "multipart/form-data")
      .sendForm(form)
      .onFailure { failure -> testContext.failNow(failure) }
      .onSuccess {
        logger.info("add-Book-res : ${it.body().toString()}")
        val insertedBookId = it.body().toString()
        testContext.verify {
          assertThat(it.body().toString()).isAlphanumeric

          deleteWebClient.delete(8090, "localhost", deleteRequestURI)
            .setTemplateParam("book_Id", insertedBookId)
            .send()
            .onFailure { failure -> testContext.failNow(failure) }
            .onSuccess {
              logger.info("test_delete_book : ${it.body().toString()}")
              val deleteRow = 1
              testContext.verify {
                assertThat(it.body().toString()).isEqualTo("" + deleteRow)

                val getWebClient = WebClient.create(vertx)
                getWebClient.get(8090, "localhost", "/books?title=".plus(title))
                  .send()
                  .onFailure { failure -> testContext.failNow(failure) }
                  .onSuccess { res ->
                    testContext.verify {
                      val bookInfos = res.body().toJsonArray()
                      logger.info("bookInfos : $bookInfos")
                      assertThat(bookInfos).isEmpty()
                      testContext.completeNow()
                    }
                  }
              }
            }
        }
      }
  }

  @Test
  @DisplayName("test_update_book")
  fun test_update_book(vertx: Vertx, testContext: VertxTestContext) {

    val counter: Int = Random.nextInt(0, 200)
    val form: MultiMap = MultiMap.caseInsensitiveMultiMap()
    val isbn: String = "Test".plus(counter)
    form.set("isbn", isbn);
    val title: String = "Test".plus(counter)
    form.set("title", title);
    form.set("description", "any test to describe my book");
    form.set("price", "64");

    val addWebClient = WebClient.create(vertx)
    addWebClient.post(8090, "localhost", "/books")
      .putHeader("content-type", "multipart/form-data")
      .sendForm(form)
      .onFailure { failure -> testContext.failNow(failure) }
      .onSuccess {
        logger.info("add-Book-res : ${it.body().toString()}")
        val insertedBookId = it.body().toString()
        testContext.verify {
          assertThat(it.body().toString()).isAlphanumeric

          val updateRequestURI: UriTemplate = UriTemplate.of("/books/{book_Id}")
          val updateWebClient = WebClient.create(vertx)
          var bookToBeUpdated = Book(0, isbn, title.plus(2), "any test to descript my book", 100)
          var bookInfoToBeMatched = BookInfo(0, isbn, title.plus(2), 100)
          updateWebClient.put(8090, "localhost", updateRequestURI)
            .setTemplateParam("book_Id", insertedBookId)
            .sendJson(bookToBeUpdated)
            .onFailure { failure -> testContext.failNow(failure) }
            .onSuccess { afterUpdatedRes ->
              logger.info("test_updated_book : ${afterUpdatedRes.body().toString()}")
              val updatedRow = 1
              testContext.verify {
                assertThat(afterUpdatedRes.body().toString()).isEqualTo("" + updatedRow)

                val getWebClient = WebClient.create(vertx)
                getWebClient.get(8090, "localhost", "/books?title=".plus(title.plus(2)))
                  .send()
                  .onFailure { failure -> testContext.failNow(failure) }
                  .onSuccess { res ->
                    testContext.verify {
                      val bookInfos = toList(res.body().toJsonArray())
                      logger.info("bookInfos : $bookInfos")
                      assertThat(bookInfos).anyMatch { bookInfo -> checkingBookMatching(bookInfo, bookInfoToBeMatched) }
                      testContext.completeNow()
                    }
                  }
              }
            }
        }
      }
  }

  @Test
  @DisplayName("test_check_isbn_exist")
  fun test_check_isbn_exist(vertx: Vertx, testContext: VertxTestContext) {

    var checkedIsbn = "zxc6"
    val webClient = WebClient.create(vertx)
    val checkRequestURI: UriTemplate = UriTemplate.of("/books/{isbn}")
    webClient.head(8090, "localhost", checkRequestURI)
      .setTemplateParam("isbn", checkedIsbn)
      .send()
      .onFailure { failure -> testContext.failNow(failure) }
      .onSuccess { res ->
        testContext.verify {
          val isIsbnExist = res.getHeader("isExist")
          logger.info("isIsbnExist : $isIsbnExist")
          assertThat(isIsbnExist).isEqualTo("true")
          testContext.completeNow()
        }
      }
  }

  @Test
  @DisplayName("test_update_title_book")
  fun test_update_title_book(vertx: Vertx, testContext: VertxTestContext) {

    val counter: Int = Random.nextInt(0, 200)
    val form: MultiMap = MultiMap.caseInsensitiveMultiMap()
    val isbn: String = "Test".plus(counter)
    form.set("isbn", isbn);
    val title: String = "Test".plus(counter)
    form.set("title", title);
    form.set("description", "any test to describe my book");
    form.set("price", "64");

    val addWebClient = WebClient.create(vertx)
    addWebClient.post(8090, "localhost", "/books")
      .putHeader("content-type", "multipart/form-data")
      .sendForm(form)
      .onFailure { failure -> testContext.failNow(failure) }
      .onSuccess {
        logger.info("add-Book-res : ${it.body().toString()}")
        val insertedBookId = it.body().toString()
        testContext.verify {
          assertThat(it.body().toString()).isAlphanumeric

          val partialUpdateRequestURI: UriTemplate = UriTemplate.of("/books/{book_Id}?new_title=".plus(title.plus(2)))
          val updateWebClient = WebClient.create(vertx)
          var bookInfoToBeMatched = BookInfo(0, isbn, title.plus(2), 64)
          updateWebClient.patch(8090, "localhost", partialUpdateRequestURI)
            .setTemplateParam("book_Id", insertedBookId)
            .send()
            .onFailure { failure -> testContext.failNow(failure) }
            .onSuccess { afterUpdatedRes ->
              logger.info("test_update_title_only : ${afterUpdatedRes.body().toString()}")
              val updatedRow = 1
              testContext.verify {
                assertThat(afterUpdatedRes.body().toString()).isEqualTo("" + updatedRow)

                val getWebClient = WebClient.create(vertx)
                getWebClient.get(8090, "localhost", "/books?title=".plus(title.plus(2)))
                  .send()
                  .onFailure { failure -> testContext.failNow(failure) }
                  .onSuccess { res ->
                    testContext.verify {
                      val bookInfos = toList(res.body().toJsonArray())
                      logger.info("bookInfos : $bookInfos")
                      assertThat(bookInfos).anyMatch { bookInfo -> checkingBookMatching(bookInfo, bookInfoToBeMatched) }
                      testContext.completeNow()
                    }
                  }
              }
            }
        }
      }
  }

  private fun checkingBookMatching(book1: BookInfo, book2: BookInfo): Boolean {
    return (book1.isbn.equals(book2.isbn) && book1.title.equals(book2.title) && book1.price == book2.price)
  }

  @Throws(Exception::class)
  private fun toList(jsonArr: JsonArray): List<BookInfo> {
    val mapper = jacksonObjectMapper()
    val list = mutableListOf<BookInfo>()
    for (i in 0 until jsonArr.size()) {
      list.add(mapper.readValue((jsonArr.get(i) as JsonObject).toString(), BookInfo::class.java))
    }
    return list
  }

}
