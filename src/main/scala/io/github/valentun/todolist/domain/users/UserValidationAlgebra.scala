package io.github.valentun.todolist.domain
package users

import cats.data.EitherT

trait UserValidationAlgebra[F[_]] {
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit]

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit]

  def owns(userId: Long, requesterId: Long): EitherT[F, WrongUserError.type, Unit]
}
