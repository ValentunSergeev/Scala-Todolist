package io.github.valentun.todolist.infrastructure.repository.inmemory

import cats._
import cats.implicits._
import io.github.valentun.todolist.domain.todos.{Todo, TodoRepositoryAlgebra}

import scala.collection.concurrent.TrieMap
import scala.util.Random

class TodoRepositoryInMemoryInterpreter[F[_]: Applicative] extends TodoRepositoryAlgebra[F] {
  private val cache = new TrieMap[Long, Todo]

  private val random = new Random

  override def create(todo: Todo, userId: Long): F[Todo] = {
    val id = random.nextLong()
    val toSave = todo.copy(id = id.some, userId = userId.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  override def update(todo: Todo): F[Option[Todo]] = todo.id.traverse { id =>
    cache.update(id, todo)
    todo.pure[F]
  }

  override def get(id: Long): F[Option[Todo]] = cache.get(id).pure[F]

  override def delete(id: Long): F[Option[Todo]] = cache.remove(id).pure[F]

  override def findByUser(userId: Long): F[List[Todo]] =
    cache.values
      .filter(todo => todo.userId.get == userId)
      .toList
      .pure[F]
}

object TodoRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new TodoRepositoryInMemoryInterpreter[F]()
}
