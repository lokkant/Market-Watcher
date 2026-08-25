package interpreters

import algebras.NotificationService
import cats.effect.{IO, Resource}
import domain.Direction.Below
import telegramium.bots.ChatId
import domain.{Alert, Price}
import org.http4s.ember.client.EmberClientBuilder
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.high.{Api, BotApi, Methods}

opaque type Token = String

object Token {
  def apply(value: String): Token = value
}

class TelegramNotificationService private (chatId: ChatId)(using api: Api[IO]) extends NotificationService[IO] {

  override def notify(alert: Alert, price: Price): IO[Unit] = {
    val message = s"${alert.symbol} is ${if alert.direction == Below then "below" else "above"} " +
      s"${alert.threshold} ${alert.baseCurrency}. Current currency: ${price.value}"

    Methods.sendMessage(chatId, text = message).exec.void
      .handleErrorWith { err =>
        IO.println(s"failed to notify about ${alert.symbol}: ${err.getMessage}")
      }
  }

}

object TelegramNotificationService {
  def apply(token: Token, chatId: ChatId): Resource[IO, TelegramNotificationService] = {
    EmberClientBuilder.default[IO].build.map { httpClient =>
      given api: Api[IO] = BotApi(httpClient, baseUrl = s"https://api.telegram.org/bot$token")
      new TelegramNotificationService(chatId)
    }
  }
}
