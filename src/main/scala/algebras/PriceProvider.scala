package algebras

import domain.{Price, Symbol}

trait PriceProvider[F[_]] {
  def fetch(symbol: Symbol): F[Price]
}
