package algebras

import domain.Price

trait PriceRepository[F[_]] {
  def save(price: Price): F[Unit]
  def history(symbol: Symbol, limit: Int): F[List[Price]]
}
