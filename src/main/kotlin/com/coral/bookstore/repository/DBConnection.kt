package coral.bookstore.bookstore.repository

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import liquibase.database.Database


class DBConnection {

  fun pgPool(vertx : Vertx): PgPool {
    val connectOptions = PgConnectOptions()
      .setPort(5432)
      .setHost("localhost")
      .setDatabase("liquibase_bookstore")
      .setUser("postgres")
      .setPassword("postgres")

    // Pool Options
    val poolOptions = PoolOptions().setMaxSize(5)

    // Create the pool from the data object
    return PgPool.pool(vertx, connectOptions, poolOptions)
  }

//  fun createPgPoolByFileProperties(vertx : Vertx): Future<PgPool> {
//    val pgPoolPromise: Promise<PgPool> = Promise.promise()
//    val config: JsonObject = vertx.fileSystem().readFileBlocking("/pg_connection.json").toJsonObject()
//
//    val connectOptions = PgConnectOptions()
//      .setPort(config.getInteger("port"))
//      .setHost(config.getString("host"))
//      .setDatabase(config.getString("database"))
//      .setUser(config.getString("user"))
//      .setPassword(config.getString("password"))
//
//    // Pool Options
//    val poolOptions = PoolOptions().setMaxSize(config.getInteger("max_pool_size"))
//
//    // Create the pool from the data object
//    pgPoolPromise.complete(PgPool.pool(vertx, connectOptions, poolOptions))
//    return pgPoolPromise.future()
//
//  }
}
