package algebras

import domain.Price

trait PriceProvider[F[_]] {
  def fetch(symbol: Symbol): F[Price]
}
