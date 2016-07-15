package services

import org.flywaydb.core.Flyway
import org.funktionale.either.Either
import org.funktionale.either.eitherTry
import org.funktionale.option.Option
import javax.sql.DataSource

class MigrationService(dataSource: Option<DataSource>) {
  private val flyway: Flyway = Flyway()
  init {
    dataSource.map {
      flyway.setDataSource(it)
    }
  }
  fun migrate(): Either<Exception, Int> = eitherTry {
    flyway.migrate()
  }
}