package services

import org.flywaydb.core.Flyway
import org.funktionale.either.Either
import org.funktionale.either.eitherTry
import javax.sql.DataSource

class MigrationService(dataSource: DataSource?) {
  private val flyway: Flyway = Flyway()
  init {
    flyway.dataSource = dataSource
  }
  fun migrate(): Either<Throwable, Int> = eitherTry {
    flyway.migrate()
  }
}