package io.github.valentun.todolist
package infrastructure.repository.inmemory

import cats.Applicative
import cats.data.OptionT
import cats.implicits._
import io.github.valentun.todolist.domain.users.{User, UserRepositoryAlgebra}
import tsec.authentication.IdentityStore

import java.util.Random
import scala.collection.concurrent.TrieMap

class UserRepositoryInMemoryInterpreter[F[_]: Applicative]
    extends UserRepositoryAlgebra[F]
    with IdentityStore[F, Long, User] {
  private val cache = new TrieMap[Long, User]

  private val random = new Random

  def create(user: User): F[User] = Applicative[F]
    .pure(user)
    .map { user =>
      val id = random.nextLong()
      val toSave = user.copy(id = id.some)
      cache += (id -> toSave)

      toSave
    }

  def update(user: User): OptionT[F, User] = OptionT {
    Applicative[F]
      .pure(user)
      .map { user =>
        user.id.map { id =>
          cache.update(id, user)

          user
        }
      }
  }

  def get(id: Long): OptionT[F, User] = OptionT {
    Applicative[F]
      .pure(id)
      .map(cache.get)
  }

  def delete(id: Long): OptionT[F, User] = OptionT {
    Applicative[F]
      .pure(id)
      .map(cache.remove)
  }

  def findByUserName(userName: String): OptionT[F, User] = OptionT {
    Applicative[F]
      .pure(userName)
      .map { userName =>
        cache.values.find(u => u.userName == userName)
      }
  }

  def list(pageSize: Int, offset: Int): F[List[User]] = Applicative[F]
    .pure((pageSize, offset))
    .map { case (pageSize, offset) =>
      cache.values.toList.slice(offset, offset + pageSize)
    }

  def deleteByUserName(userName: String): OptionT[F, User] =
    OptionT.fromOption(
      for {
        user <- cache.values.find(u => u.userName == userName)
        removed <- cache.remove(user.id.get)
      } yield removed,
    )
}

object UserRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() =
    new UserRepositoryInMemoryInterpreter[F]
}
