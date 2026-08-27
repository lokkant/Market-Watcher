package interpreters

import cats.effect.IO
import domain.Symbol
import io.circe.parser.parse
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.client.Client
import org.http4s.{HttpApp, Request, Response, Status}
import org.typelevel.ci.*

import java.time.Instant

class Http4sPriceProviderSpec extends CatsEffectSuite {

  private def fakeClient(handler: Request[IO] => IO[Response[IO]]): Client[IO] =
    Client.fromHttpApp(HttpApp(handler))

  test("fetch routes a fiat symbol to the exchangerate-api pair endpoint") {
    val client = fakeClient { req =>
      assertEquals(req.uri.path.toString, "/v6/test-key/pair/USD/EUR")
      IO(Response[IO](Status.Ok).withEntity(
        parse("""{"result":"success","conversion_rate":0.8572,"time_last_update_unix":1700000000}""").toOption.get
      ))
    }
    val provider = Http4sPriceProvider.fromClient(client, "test-key", "test-key")

    provider.fetch(Symbol("EUR")).map { price =>
      assertEquals(price.symbol, Symbol("EUR"))
      assertEquals(price.baseCurrency, Symbol("USD"))
      assertEquals(price.value, BigDecimal("0.8572"))
      assertEquals(price.time, Instant.ofEpochSecond(1700000000L))
    }
  }

  test("fetch routes a crypto symbol to the CoinMarketCap quotes endpoint") {
    val client = fakeClient { req =>
      assertEquals(req.uri.path.toString, "/v1/cryptocurrency/quotes/latest")
      assertEquals(req.headers.get(ci"X-CMC_PRO_API_KEY").map(_.head.value), Some("test-key"))
      IO(Response[IO](Status.Ok).withEntity(
        parse(
          """{"data":{"BTC":{"quote":{"USD":{"price":65000.5,"last_updated":"2024-01-01T00:00:00.000Z"}}}}}"""
        ).toOption.get
      ))
    }
    val provider = Http4sPriceProvider.fromClient(client, "test-key", "test-key")

    provider.fetch(Symbol("BTC")).map { price =>
      assertEquals(price.symbol, Symbol("BTC"))
      assertEquals(price.baseCurrency, Symbol("USD"))
      assertEquals(price.value, BigDecimal("65000.5"))
      assertEquals(price.time, Instant.parse("2024-01-01T00:00:00.000Z"))
    }
  }

  test("fetch fails when the CoinMarketCap response is missing the requested symbol") {
    val client = fakeClient { _ =>
      IO(Response[IO](Status.Ok).withEntity(parse("""{"data":{}}""").toOption.get))
    }
    val provider = Http4sPriceProvider.fromClient(client, "test-key", "test-key")

    provider.fetch(Symbol("BTC")).attempt.map(result => assert(result.isLeft))
  }
}
