package domain

import java.util.UUID

opaque type AlertId = UUID

object AlertId {
  def apply(): AlertId = UUID.randomUUID()
  def apply(value: UUID): AlertId = value
  extension (alertId: AlertId) def value: UUID = alertId
}

enum Direction {
  case Below, Above
}

case class Alert(
    id: AlertId,
    symbol: Symbol,
    baseCurrency: Symbol,
    threshold: BigDecimal,
    direction: Direction,
    isActive: Boolean
)

object Alert {
  def apply(
      symbol: Symbol,
      baseCurrency: Symbol,
      threshold: BigDecimal,
      direction: Direction,
      isActive: Boolean = true
  ): Alert = 
    new Alert(AlertId(), symbol, baseCurrency, threshold, direction, isActive)
}
