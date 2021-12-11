package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import domain.users.{Role, User, UserRepositoryAlgebra}
import io.github.pauljamescleary.petstore.infrastructure.repository.doobie.SQLPagination._
import tsec.authentication.IdentityStore

private object UserSQL {

  implicit val roleMeta: Meta[Role] =
    Meta[String].timap(x => Role.unsafeFromRepr(x))(x => x.roleRepr)

  def insert(user: User): Update0 = sql"""
    INSERT INTO USERS (USER_NAME, EMAIL, HASH, ROLE)
    VALUES (${user.userName}, ${user.email}, ${user.hash}, ${user.role})
  """.update

  def update(user: User, id: Long): Update0 = sql"""
    UPDATE USERS
    SET EMAIL = ${user.email}, HASH = ${user.hash}, ROLE = ${user.role}
    WHERE ID = $id
  """.update

  def select(userId: Long): Query0[User] = sql"""
    SELECT USER_NAME, EMAIL, HASH, ID, ROLE
    FROM USERS
    WHERE ID = $userId
  """.query

  def byUserName(userName: String): Query0[User] = sql"""
    SELECT USER_NAME, EMAIL, HASH, ID, ROLE
    FROM USERS
    WHERE USER_NAME = $userName
  """.query[User]

  def delete(userId: Long): Update0 = sql"""
    DELETE FROM USERS WHERE ID = $userId
  """.update

  val selectAll: Query0[User] = sql"""
    SELECT USER_NAME, EMAIL, HASH, ID, ROLE
    FROM USERS
  """.query
}

class DoobieUserRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends UserRepositoryAlgebra[F]
    with IdentityStore[F, Long, User] { self =>
  import UserSQL._

  def create(user: User): F[User] =
    insert(user).withUniqueGeneratedKeys[Long]("ID").map(id => user.copy(id = id.some)).transact(xa)

  def update(user: User): OptionT[F, User] =
    OptionT.fromOption[F](user.id).semiflatMap { id =>
      UserSQL.update(user, id).run.transact(xa).as(user)
    }

  def get(userId: Long): OptionT[F, User] = OptionT(select(userId).option.transact(xa))

  def findByUserName(userName: String): OptionT[F, User] =
    OptionT(byUserName(userName).option.transact(xa))

  def delete(userId: Long): OptionT[F, User] =
    get(userId).semiflatMap(user => UserSQL.delete(userId).run.transact(xa).as(user))

  def deleteByUserName(userName: String): OptionT[F, User] =
    findByUserName(userName).mapFilter(_.id).flatMap(delete)

  def list(pageSize: Int, offset: Int): F[List[User]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter(xa)
}
