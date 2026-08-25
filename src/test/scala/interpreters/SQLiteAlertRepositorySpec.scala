package interpreters

import cats.effect.{IO, Resource}
import domain.{Alert, Direction, Symbol}
import doobie.Transactor
import doobie.implicits.*
import munit.CatsEffectSuite

import java.nio.file.Files

class SQLiteAlertRepositorySpec extends CatsEffectSuite {

  private def repositoryResource: Resource[IO, SQLiteAlertRepository] =
    for {
      dbFile <- Resource.make(IO.blocking(Files.createTempFile("alerts-test", ".db"))) { path =>
        IO.blocking(Files.deleteIfExists(path)).void
      }
      transactor = Transactor.fromDriverManager[IO](
        driver = "org.sqlite.JDBC",
        url = s"jdbc:sqlite:${dbFile.toAbsolutePath}",
        logHandler = None
      )
      _ <- Resource.eval(createSchema(transactor))
    } yield new SQLiteAlertRepository(transactor)

  private def createSchema(transactor: Transactor[IO]): IO[Unit] =
    sql"""
      CREATE TABLE Alerts (
        id TEXT NOT NULL PRIMARY KEY,
        symbol TEXT NOT NULL,
        baseCurrency TEXT NOT NULL,
        threshold REAL NOT NULL,
        direction TEXT NOT NULL,
        isActive INTEGER NOT NULL
      )
    """.update.run.transact(transactor).void

  test("add persists an alert that findActiveFor can then find") {
    repositoryResource.use { repository =>
      val alert = Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("1.10"), direction = Direction.Above)
      for {
        _       <- repository.add(alert)
        results <- repository.findActiveFor(Symbol("EUR"))
      } yield assertEquals(results, List(alert))
    }
  }

  test("findActiveFor only returns alerts for the requested symbol") {
    repositoryResource.use { repository =>
      val eurAlert = Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("1.10"), direction = Direction.Above)
      val btcAlert = Alert(Symbol("BTC"), Symbol("USD"), threshold = BigDecimal("50000"), direction = Direction.Below)
      for {
        _       <- repository.add(eurAlert)
        _       <- repository.add(btcAlert)
        results <- repository.findActiveFor(Symbol("EUR"))
      } yield assertEquals(results, List(eurAlert))
    }
  }

  test("findActiveFor excludes inactive alerts") {
    repositoryResource.use { repository =>
      val activeAlert = Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("1.10"), direction = Direction.Above)
      val inactiveAlert =
        Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("0.90"), direction = Direction.Below, isActive = false)
      for {
        _       <- repository.add(activeAlert)
        _       <- repository.add(inactiveAlert)
        results <- repository.findActiveFor(Symbol("EUR"))
      } yield assertEquals(results, List(activeAlert))
    }
  }

  test("findActiveFor returns an empty list when there are no matching alerts") {
    repositoryResource.use { repository =>
      repository.findActiveFor(Symbol("EUR")).map(results => assertEquals(results, Nil))
    }
  }

  test("deactivate makes an alert disappear from findActiveFor") {
    repositoryResource.use { repository =>
      val alert = Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("1.10"), direction = Direction.Above)
      for {
        _       <- repository.add(alert)
        _       <- repository.deactivate(alert.id)
        results <- repository.findActiveFor(Symbol("EUR"))
      } yield assertEquals(results, Nil)
    }
  }

  test("deactivate only affects the targeted alert, not others for the same symbol") {
    repositoryResource.use { repository =>
      val toDeactivate = Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("1.10"), direction = Direction.Above)
      val untouched    = Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("0.90"), direction = Direction.Below)
      for {
        _       <- repository.add(toDeactivate)
        _       <- repository.add(untouched)
        _       <- repository.deactivate(toDeactivate.id)
        results <- repository.findActiveFor(Symbol("EUR"))
      } yield assertEquals(results, List(untouched))
    }
  }

  test("deactivate on an unknown alert id is a no-op") {
    repositoryResource.use { repository =>
      val unknownAlert = Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("1.10"), direction = Direction.Above)
      repository.deactivate(unknownAlert.id)
    }
  }
}
