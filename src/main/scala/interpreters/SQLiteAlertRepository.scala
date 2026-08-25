package interpreters

import algebras.AlertRepository
import cats.effect.IO
import domain.{Alert, AlertId, Direction, Symbol}
import doobie.Transactor
import doobie.implicits.*
import doobie.*

import java.util.UUID

class SQLiteAlertRepository(transactor: Transactor[IO]) extends AlertRepository[IO] {
  import SQLiteAlertRepository.given

  override def add(alert: Alert): IO[Unit] =
    sql"INSERT INTO Alerts (id, symbol, baseCurrency, threshold, direction, isActive) VALUES ($alert)"
      .update
      .run
      .transact(transactor)
      .void

  override def delete(alertId: AlertId): IO[Unit] =
    sql"DELETE FROM Alerts WHERE id = $alertId"
      .update
      .run
      .transact(transactor)
      .void

  override def activate(alertId: AlertId): IO[Unit] =
    sql"UPDATE Alerts SET isActive = true WHERE id = $alertId"
      .update
      .run
      .transact(transactor)
      .void

  override def deactivate(alertId: AlertId): IO[Unit] =
    sql"UPDATE Alerts SET isActive = false WHERE id = $alertId"
      .update
      .run
      .transact(transactor)
      .void

  override def findActiveFor(symbol: Symbol): IO[List[Alert]] =
    sql"SELECT id, symbol, baseCurrency, threshold, direction, isActive FROM Alerts WHERE symbol = $symbol AND isActive = true"
      .query[Alert]
      .to[List]
      .transact(transactor)
}

object SQLiteAlertRepository {
  given Meta[Symbol] = Meta[String].timap(Symbol.apply)(_.value)
  given Meta[AlertId] = Meta[String].timap(str => AlertId(UUID.fromString(str)))(_.value.toString)
  given Meta[Direction] = Meta[String].timap(Direction.valueOf)(_.toString)

  def apply(dbPath: String): SQLiteAlertRepository = {
    val transactor = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:$dbPath",
      logHandler = None
    )

    new SQLiteAlertRepository(transactor)
  }
}
