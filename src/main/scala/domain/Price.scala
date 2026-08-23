package domain

import java.time.Instant

opaque type Symbol = String

case class Price(symbol: Symbol, value: BigDecimal, time: Instant)
