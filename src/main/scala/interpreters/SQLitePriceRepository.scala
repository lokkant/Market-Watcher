package interpreters

import algebras.PriceRepository
import cats.effect.IO
import domain.Price
import doobie.Transactor
import doobie.implicits.*
import doobie.*
import domain.Symbol

import java.time.Instant

class SQLitePriceRepository private(transactor: Transactor[IO]) extends PriceRepository[IO] {
  import SQLitePriceRepository.given

  override def save(price: Price): IO[Unit] =
    sql"INSERT OR IGNORE INTO Prices (symbol, baseCurrency, price, unix_time_millis) VALUES ($price)"
      .update
      .run
      .transact(transactor)
      .void

  override def history(symbol: Symbol, baseCurrency: Symbol, limit: Int): IO[List[Price]] =
    sql"SELECT symbol, baseCurrency, price, unix_time_millis FROM Prices WHERE symbol = $symbol AND baseCurrency = ${baseCurrency.value} ORDER BY unix_time_millis DESC LIMIT $limit"
      .query[Price]
      .to[List]
      .transact(transactor)
}

object SQLitePriceRepository {
  given Meta[Symbol] = Meta[String].timap(Symbol.apply)(_.value)
  given Meta[Instant] = Meta[Long].timap(Instant.ofEpochMilli)(_.toEpochMilli())

  def apply(dbPath: String): SQLitePriceRepository = {
    val transactor = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:$dbPath",
      logHandler = None
    )

    new SQLitePriceRepository(transactor)
  }
}
