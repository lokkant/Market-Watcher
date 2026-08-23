package algebras

import domain.Alert

trait AlertRepository[F[_]] {
  def add(alert: Alert): F[Unit]
  def deactivate(alert: Alert): F[Unit]
  def findActiveFor(symbol: Symbol): F[List[Alert]]
}
