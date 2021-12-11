package io.github.pauljamescleary.petstore.domain.todos

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._

class TodoValidationInterpreter[F[_]: Applicative](repository: TodoRepositoryAlgebra[F])
    extends TodoValidationAlgebra[F] {

  override def exists(todoId: Option[Long]): EitherT[F, TodoNotFoundError.type, Unit] = EitherT {
    todoId match {
      case Some(id) =>
        repository.get(id).map {
          case Some(_) => Right(())
          case _ => Left(TodoNotFoundError)
        }
      case _ =>
        Either.left[TodoNotFoundError.type, Unit](TodoNotFoundError).pure[F]
    }
  }

  override def belongs(todoId: Long, userId: Long): EitherT[F, WrongUserError.type, Unit] =
    EitherT {
      repository.get(todoId).map { todo =>
        if (todo.get.userId.get == userId) {
          Right(())
        } else {
          Left(WrongUserError)
        }
      }
    }
}

object TodoValidationInterpreter {
  def apply[F[_]: Applicative](repository: TodoRepositoryAlgebra[F]) =
    new TodoValidationInterpreter[F](repository)
}
