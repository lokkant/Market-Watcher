package algebras

import domain.{Price, Symbol}

trait PriceRepository[F[_]] {
  def save(price: Price): F[Unit]
  def history(symbol: Symbol, baseCurrency: Symbol, limit: Int): F[List[Price]]
}
