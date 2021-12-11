package io.github.pauljamescleary.petstore.domain.users

import cats.Applicative
import tsec.authorization.AuthorizationInfo

case class User(
    userName: String,
    email: String,
    hash: String,
    id: Option[Long] = None,
    role: Role,
)

object User {
  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, User] =
    (u: User) => F.pure(u.role)
}
