package domain

import java.time.Instant

opaque type Symbol = String

object Symbol {
  def apply(value: String): Symbol = value
  extension (symbol: Symbol) def value: String = symbol
}

case class Price(symbol: Symbol, baseCurrency: Symbol, value: BigDecimal, time: Instant)
