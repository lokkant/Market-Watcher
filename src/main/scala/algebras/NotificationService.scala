package algebras

import domain.{Alert, Price}

trait NotificationService[F[_]] {
  def notify(alert: Alert, price: Price): F[Unit]
}
