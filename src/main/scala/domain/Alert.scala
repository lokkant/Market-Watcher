package domain

import java.util.UUID

opaque type AlertId = UUID

enum Direction {
  case Below, Above
}

case class Alert(id: AlertId, symbol: Symbol, threshold: BigDecimal, direction: Direction, isActive: Boolean)
