package interpreters

import cats.effect.{IO, Ref}
import domain.{Alert, Direction, Price, Symbol}
import munit.CatsEffectSuite
import org.http4s.client.Client
import org.http4s.{HttpApp, Request, Response, Status}
import telegramium.bots.ChatIntId

import java.time.Instant

class TelegramNotificationServiceSpec extends CatsEffectSuite {

  private val alert = Alert(Symbol("EUR"), Symbol("USD"), threshold = BigDecimal("0.90"), direction = Direction.Below)
  private val price = Price(Symbol("EUR"), Symbol("USD"), BigDecimal("0.85"), Instant.parse("2026-01-01T00:00:00Z"))

  test("notify sends a request to Telegram's sendMessage endpoint for the target chat") {
    for {
      requests <- Ref.of[IO, List[Request[IO]]](Nil)
      client = Client.fromHttpApp(HttpApp[IO] { req =>
        req.bodyText.compile.string.flatMap { body =>
          requests.update(_ :+ req) *> IO(Response[IO](Status.Unauthorized))
        }
      })
      notifier = TelegramNotificationService.fromClient(client, Token("test-token"), ChatIntId(12345L))
      _        <- notifier.notify(alert, price)
      captured <- requests.get
    } yield {
      assertEquals(captured.size, 1)
      assert(captured.head.uri.toString.contains("test-token"))
      assert(captured.head.uri.toString.contains("sendMessage"))
    }
  }

  test("notify does not fail when the Telegram call is rejected") {
    val client = Client.fromHttpApp(HttpApp[IO](_ => IO(Response[IO](Status.Unauthorized))))
    val notifier = TelegramNotificationService.fromClient(client, Token("bad-token"), ChatIntId(12345L))

    notifier.notify(alert, price).attempt.map(result => assertEquals(result, Right(())))
  }
}
