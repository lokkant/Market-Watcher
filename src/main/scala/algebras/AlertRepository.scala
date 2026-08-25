package algebras

import domain.{Alert, AlertId, Symbol}

trait AlertRepository[F[_]] {
  def add(alert: Alert): F[Unit]
  def delete(alertId: AlertId): F[Unit]
  def activate(alertId: AlertId): F[Unit]
  def deactivate(alertId: AlertId): F[Unit]
  def findActiveFor(symbol: Symbol): F[List[Alert]]
}
