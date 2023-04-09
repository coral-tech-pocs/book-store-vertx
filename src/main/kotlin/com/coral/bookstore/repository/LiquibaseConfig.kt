package com.coral.bookstore.repository

import com.coral.bookstore.MainVerticle
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
        val config: JsonObject = vertx.fileSystem().readFileBlocking("DB_Connection.json").toJsonObject()
        val source = PGSimpleDataSource()
        source.serverNames = arrayOf(config.getString("host"))
        source.databaseName = config.getString("database")
        source.user = config.getString("user")
        source.password = config.getString("password")

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
