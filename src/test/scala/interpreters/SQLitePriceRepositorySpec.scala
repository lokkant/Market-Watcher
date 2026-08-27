package interpreters

import cats.effect.{IO, Resource}
import domain.{Price, Symbol}
import doobie.Transactor
import doobie.implicits.*
import munit.CatsEffectSuite

import java.nio.file.Files
import java.time.Instant

class SQLitePriceRepositorySpec extends CatsEffectSuite {

  private def repositoryResource: Resource[IO, SQLitePriceRepository] =
    for {
      dbFile <- Resource.make(IO.blocking(Files.createTempFile("prices-test", ".db"))) { path =>
        IO.blocking(Files.deleteIfExists(path)).void
      }
      transactor = Transactor.fromDriverManager[IO](
        driver = "org.sqlite.JDBC",
        url = s"jdbc:sqlite:${dbFile.toAbsolutePath}",
        logHandler = None
      )
      _ <- Resource.eval(createSchema(transactor))
    } yield SQLitePriceRepository(dbFile.toAbsolutePath.toString)

  private def createSchema(transactor: Transactor[IO]): IO[Unit] =
    for {
      _ <- sql"""
        CREATE TABLE Prices (
          symbol TEXT NOT NULL,
          baseCurrency TEXT NOT NULL,
          price REAL NOT NULL,
          unix_time_millis INTEGER NOT NULL
        )
      """.update.run.transact(transactor)
      _ <- sql"CREATE UNIQUE INDEX idx_prices_unique ON Prices(symbol, baseCurrency, unix_time_millis)"
        .update.run.transact(transactor)
    } yield ()

  private val t1 = Instant.parse("2026-01-01T00:00:00Z")
  private val t2 = Instant.parse("2026-01-02T00:00:00Z")
  private val t3 = Instant.parse("2026-01-03T00:00:00Z")

  test("save persists a price that history can then find") {
    repositoryResource.use { repository =>
      val price = Price(Symbol("EUR"), Symbol("USD"), BigDecimal("0.8572"), t1)
      for {
        _       <- repository.save(price)
        results <- repository.history(Symbol("EUR"), Symbol("USD"), limit = 10)
      } yield assertEquals(results, List(price))
    }
  }

  test("save is idempotent for the same symbol/baseCurrency/time") {
    repositoryResource.use { repository =>
      val price = Price(Symbol("EUR"), Symbol("USD"), BigDecimal("0.8572"), t1)
      for {
        _       <- repository.save(price)
        _       <- repository.save(price)
        results <- repository.history(Symbol("EUR"), Symbol("USD"), limit = 10)
      } yield assertEquals(results, List(price))
    }
  }

  test("history filters by both symbol and baseCurrency") {
    repositoryResource.use { repository =>
      val eurUsd = Price(Symbol("EUR"), Symbol("USD"), BigDecimal("0.8572"), t1)
      val eurGbp = Price(Symbol("EUR"), Symbol("GBP"), BigDecimal("0.75"), t1)
      val btcUsd = Price(Symbol("BTC"), Symbol("USD"), BigDecimal("65000"), t1)
      for {
        _       <- repository.save(eurUsd)
        _       <- repository.save(eurGbp)
        _       <- repository.save(btcUsd)
        results <- repository.history(Symbol("EUR"), Symbol("USD"), limit = 10)
      } yield assertEquals(results, List(eurUsd))
    }
  }

  test("history orders results by time descending and respects the limit") {
    repositoryResource.use { repository =>
      val oldest  = Price(Symbol("EUR"), Symbol("USD"), BigDecimal("0.80"), t1)
      val middle  = Price(Symbol("EUR"), Symbol("USD"), BigDecimal("0.85"), t2)
      val newest  = Price(Symbol("EUR"), Symbol("USD"), BigDecimal("0.90"), t3)
      for {
        _       <- repository.save(oldest)
        _       <- repository.save(middle)
        _       <- repository.save(newest)
        results <- repository.history(Symbol("EUR"), Symbol("USD"), limit = 2)
      } yield assertEquals(results, List(newest, middle))
    }
  }

  test("history returns an empty list when there is no matching data") {
    repositoryResource.use { repository =>
      repository.history(Symbol("EUR"), Symbol("USD"), limit = 10).map(results => assertEquals(results, Nil))
    }
  }
}
