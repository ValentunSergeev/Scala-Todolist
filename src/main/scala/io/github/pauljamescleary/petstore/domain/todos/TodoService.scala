package io.github.pauljamescleary.petstore.domain.todos

import cats.Functor
import cats.data._
import cats.Monad
import cats.syntax.all._

class TodoService[F[_]](
    repository: TodoRepositoryAlgebra[F],
    validation: TodoValidationAlgebra[F],
) {
  def create(todo: Todo, userId: Long): F[Todo] = repository.create(todo, userId)

  def update(todo: Todo, requesterId: Long)(implicit M: Monad[F]): EitherT[F, TodoValidationError, Todo] =
    for {
      _ <- validation.exists(todo.id)
      _ <- validation.belongs(todo.id.get, requesterId)
      saved <- EitherT.fromOptionF[F, TodoValidationError, Todo](repository.update(todo), TodoNotFoundError)
    } yield saved

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, TodoNotFoundError.type, Todo] =
    EitherT.fromOptionF(repository.get(id), TodoNotFoundError)

  def delete(id: Long)(implicit F: Functor[F]): F[Unit] =
    repository.delete(id).as(())

  def findByUser(userId: Long): F[List[Todo]] = repository.findByUser(userId)
}

object TodoService {
  def apply[F[_]](
      repository: TodoRepositoryAlgebra[F],
      validation: TodoValidationAlgebra[F],
  ): TodoService[F] =
    new TodoService[F](repository, validation)
}
