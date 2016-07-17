package services

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import models.AuthInfo
import models.DatabaseUser
import org.funktionale.either.eitherTry
import org.funktionale.option.getOrElse
import org.funktionale.option.toOption
import org.mindrot.jbcrypt.BCrypt

class DatabaseAuthProvider(val dataSource: HikariDataSource, val jsonMapper: ObjectMapper) : AuthProvider {
  override fun authenticate(authInfoJson: JsonObject?, resultHandler: Handler<AsyncResult<User>>?) {
    val authInfo = jsonMapper.readValue(authInfoJson?.encode(), AuthInfo::class.java)
    val userT = eitherTry { using(sessionOf(dataSource)) { session ->
      val query = queryOf("select * from users where user_code = ?", authInfo.username)
      val maybeUser = query.map { DatabaseUser.fromDb(it) }.asSingle.
          runWithSession(session).toOption()
      maybeUser.getOrElse { throw Exception("User ${authInfo.username} not found!") }
    }}
    userT.fold({ exc ->
      val result = CompositeFuture.factory.failedFuture<User>(exc)
      resultHandler?.handle(result)
    }, { dbUser ->
      val isValid = BCrypt.checkpw(authInfo.password, dbUser.passwordHash)
      val result = if (isValid) {
        CompositeFuture.factory.succeededFuture(dbUser as User)
      } else {
        CompositeFuture.factory.failureFuture("Password is not valid!")
      }
      resultHandler?.handle(result)
    })
  }
}