package com.coral.bookstore.repository

import com.coral.bookstore.MainVerticle
import io.vertx.core.Context
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.postgresql.ds.PGSimpleDataSource
import java.util.logging.Logger

class LiquibaseConfig {

  companion object {
    private val LOGGER = Logger.getLogger(LiquibaseConfig::class.java.name)
    fun runLiquibaseScripts(vertx : Vertx, promise: Promise<Any>) {
      try {
        val context: Context = vertx.orCreateContext
        val source = PGSimpleDataSource()

        println("context.config().getString(\"Port\"): "+context.config().getString("port"))
        source.portNumbers = intArrayOf(context.config().getInteger("port"))
        source.serverNames = arrayOf(context.config().getString("host"))
        source.databaseName = context.config().getString("database")
        source.user = context.config().getString("user")
        source.password = context.config().getString("password")

        val database =
          DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(source.connection))

        val changelog = "/liquibase/db.changelog-master.xml"
        val liquibase = Liquibase(changelog, ClassLoaderResourceAccessor(), database)
        liquibase.update()
        promise.complete()
      } catch (e: Exception) {
        LOGGER.info("Error : $e")
        promise.fail(e)
      }
    }

  }

}
