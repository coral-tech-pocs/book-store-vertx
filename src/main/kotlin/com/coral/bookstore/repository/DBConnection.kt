package coral.bookstore.bookstore.repository

import com.coral.bookstore.MainVerticle
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import liquibase.database.Database
import java.util.logging.Logger

class DBConnection {

  private val logger = Logger.getLogger(DBConnection::class.java.name)

  fun pgPool(vertx: Vertx): PgPool {
    val context: Context = vertx.orCreateContext
    val activeProfile: String = context.config().getString("active_profile", "TEST")
    logger.info("DBConnection - active Profile : ".plus(activeProfile))

    val connectOptions = PgConnectOptions()
      .setPort(context.config().getInteger("port"))
      .setHost(context.config().getString("host"))
      .setDatabase(context.config().getString("database"))
      .setUser(context.config().getString("user"))
      .setPassword(context.config().getString("password"))
    // Pool Options
    val poolOptions = PoolOptions()
      .setMaxSize(context.config().getInteger("max_pool_size"))
//      .setShared(true)

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
