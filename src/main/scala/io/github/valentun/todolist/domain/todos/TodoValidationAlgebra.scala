package io.github.valentun.todolist.domain.todos

import cats.data.EitherT

trait TodoValidationAlgebra[F[_]] {

  def exists(todoId: Option[Long]): EitherT[F, TodoNotFoundError.type, Unit]

  def belongs(todoId: Long, userId: Long): EitherT[F, WrongUserError.type, Unit]
}

sealed trait TodoValidationError extends Product with Serializable
case object TodoNotFoundError extends TodoValidationError
case object WrongUserError extends TodoValidationError