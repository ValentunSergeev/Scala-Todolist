package io.github.valentun.todolist.infrastructure.repository.inmemory

import cats.Applicative
import cats.effect.Async
import cats.implicits._
import io.github.valentun.todolist.domain.todos.{Todo, TodoRepositoryAlgebra}

import scala.collection.concurrent.TrieMap
import scala.util.Random

class TodoRepositoryInMemoryInterpreter[F[_]: Applicative] extends TodoRepositoryAlgebra[F] {
  private val cache = new TrieMap[Long, Todo]

  private val random = new Random

  override def create(todo: Todo, userId: Long): F[Todo] = Applicative[F]
    .pure((todo, userId))
    .map { case (todo, userId) =>
      val id = random.nextLong()
      val toSave = todo.copy(id = id.some, userId = userId.some)
      cache += (id -> toSave)

      toSave
    }

  override def update(todo: Todo): F[Option[Todo]] = Applicative[F]
    .pure(todo)
    .map { todo =>
      todo.id.map { id =>
        cache.update(id, todo)

        todo
      }
    }

  override def get(id: Long): F[Option[Todo]] = Applicative[F]
    .pure(id)
    .map(cache.get)

  override def delete(id: Long): F[Option[Todo]] = Applicative[F]
    .pure(id)
    .map(cache.remove)

  override def findByUser(userId: Long): F[List[Todo]] = Applicative[F]
    .pure(userId)
    .map { userId =>
      cache.values
        .filter(todo => todo.userId.get == userId)
        .toList
    }
}

object TodoRepositoryInMemoryInterpreter {
  def apply[F[_]: Async]() = new TodoRepositoryInMemoryInterpreter[F]()
}
