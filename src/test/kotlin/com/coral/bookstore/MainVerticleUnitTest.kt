//package com.coral.bookstore
//
//import com.coral.bookstore.repository.BookRepository
//import com.coral.bookstore.service.BookHandler
//import coral.bookstore.bookstore.models.BookInfo
//import io.vertx.core.Handler
//import io.vertx.core.Vertx
//import io.vertx.core.http.HttpServerRequest
//import io.vertx.core.http.HttpServerResponse
//import io.vertx.ext.web.Router
//import io.vertx.ext.web.RoutingContext
//import io.vertx.junit5.VertxExtension
//import io.vertx.junit5.VertxTestContext
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.extension.ExtendWith
//import org.mockito.Mock
//import org.mockito.Mockito
//import org.mockito.kotlin.doReturn
//
//
//@ExtendWith(VertxExtension::class)
//class MainVerticleUnitTest {
//
//  private lateinit var bookService: BookHandler
//  private lateinit var bookRepository: BookRepository
//
//  @Mock
//  lateinit var event: RoutingContext
//  @Mock
//  lateinit var request: HttpServerRequest
//  @Mock
//  lateinit var response: HttpServerResponse
//
//  @BeforeEach
//  fun deploy_verticle(vertx: Vertx, testContext: VertxTestContext) {
//    vertx.deployVerticle(MainVerticle(), testContext.succeeding<String> { _ -> testContext.completeNow() })
//    bookService = BookHandler(vertx)
//    bookRepository = Mockito.mock(BookRepository::class.java)
//    Router.router(vertx).route().handler(MyHandler())
//    Mockito.`when`(event.request()).doReturn(request)
//    Mockito.`when`(event.response()).doReturn(response)
//  }
//
//  @Test
//  @DisplayName("test_return_all_books")
//  fun test_return_all_books(vertx: Vertx, testContext: VertxTestContext) {
//    //GIVEN
//    var bookInfo1 = BookInfo(1, "zxc1", "Test 1", 51)
//    var bookInfo2 = BookInfo(2, "zxc2", "Test 2", 52)
//    var bookInfo3 = BookInfo(3, "zxc3", "Test 3", 53)
//    val list = listOf(bookInfo1, bookInfo2, bookInfo3)
//    MyHandler().handle(event)
//
//    //WHEN
//    Mockito.`when`(bookService.list(event)).thenReturn(list)
//
//  }
//
//  internal class MyHandler : Handler<RoutingContext> {
//    override fun handle(event: RoutingContext) {
//      val request: HttpServerRequest = event.request()
//      val response: HttpServerResponse = event.response()
//      // Some code to test
//      response?.statusCode = 200
//      response?.end()
//    }
//  }
//
//}
