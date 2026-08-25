package interpreters

import algebras.PriceProvider
import cats.effect.{IO, Resource}
import domain.{FiatCurrencyCodes, Price, Symbol}
import io.circe.{Decoder, Json}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{Header, Method, Request, Uri}
import org.typelevel.ci.CIString
import java.time.Instant

class Http4sPriceProvider private (
    client: Client[IO],
    exchangeRateApiKey: String,
    coinMarketCapApiKey: String,
    baseCurrency: Symbol = Symbol("USD")
) extends PriceProvider[IO] {

  private case class PairConversionResponse(conversionRate: BigDecimal, timeLastUpdateUnix: Long)

  private given Decoder[PairConversionResponse] =
    Decoder.forProduct2("conversion_rate", "time_last_update_unix")(PairConversionResponse.apply)

  override def fetch(symbol: Symbol): IO[Price] =
    if (FiatCurrencyCodes.codes.contains(symbol.value.toUpperCase)) fetchFiat(symbol) else fetchCrypto(symbol)

  private def fetchFiat(symbol: Symbol): IO[Price] = {
    val targetCurrency = symbol.value
    val uri            =
      Uri.unsafeFromString(s"https://v6.exchangerate-api.com/v6/$exchangeRateApiKey/pair/$baseCurrency/$targetCurrency")
    client.expect[PairConversionResponse](uri).map { response =>
      Price(symbol, baseCurrency, response.conversionRate, Instant.ofEpochSecond(response.timeLastUpdateUnix))
    }
  }

  private def fetchCrypto(symbol: Symbol): IO[Price] = {
    val targetCurrency = symbol.value.toUpperCase
    val uri            = Uri
      .unsafeFromString("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest")
      .withQueryParam("symbol", targetCurrency)
      .withQueryParam("convert", baseCurrency.value)
    val request = Request[IO](Method.GET, uri)
      .putHeaders(Header.Raw(CIString("X-CMC_PRO_API_KEY"), coinMarketCapApiKey))

    client.expect[Json](request).flatMap { json =>
      val quote =
        json.hcursor.downField("data").downField(targetCurrency).downField("quote").downField(baseCurrency.value)
      val result = for {
        price       <- quote.downField("price").as[BigDecimal]
        lastUpdated <- quote.downField("last_updated").as[String]
      } yield Price(symbol, baseCurrency, price, Instant.parse(lastUpdated))
      IO.fromEither(result)
    }
  }
}

object Http4sPriceProvider {
  def apply(
      exchangeRateApiKey: String,
      coinMarketCapApiKey: String,
      baseCurrency: Symbol = Symbol("USD")
  ): Resource[IO, Http4sPriceProvider] =
    EmberClientBuilder.default[IO].build.map(new Http4sPriceProvider(
      _,
      exchangeRateApiKey,
      coinMarketCapApiKey,
      baseCurrency
    ))
}
