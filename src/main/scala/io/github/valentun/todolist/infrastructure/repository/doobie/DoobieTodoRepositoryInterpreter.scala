package io.github.valentun.todolist.infrastructure.repository.doobie

import cats.data._
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.github.valentun.todolist.domain.todos.{Todo, TodoPriority, TodoRepositoryAlgebra}

private object TodoSQL {

  implicit val PriorityMeta: Meta[TodoPriority] =
    Meta[String].timap(TodoPriority.withName)(_.entryName)

  def insert(todo: Todo, userId: Long): Update0 = sql"""
    INSERT INTO TODOS (NAME, CONTENT, PRIORITY, ID, USER_ID)
    VALUES (${todo.name}, ${todo.content}, ${todo.priority}, ${todo.id}, $userId)
  """.update

  def update(todo: Todo, id: Long): Update0 = sql"""
    UPDATE TODOS
    SET NAME = ${todo.name}, CONTENT = ${todo.content}, PRIORITY = ${todo.priority}
    WHERE id = $id
  """.update

  def select(id: Long): Query0[Todo] = sql"""
    SELECT NAME, CONTENT, PRIORITY, ID, USER_ID
    FROM TODOS
    WHERE ID = $id
  """.query

  def delete(id: Long): Update0 = sql"""
    DELETE FROM TODOS WHERE ID = $id
  """.update

  def selectByUser(userId: Long): Query0[Todo] = sql"""
    SELECT NAME, CONTENT, PRIORITY, ID, USER_ID
    FROM TODOS
    WHERE USER_ID = $userId
  """.query
}

class DoobieTodoRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val transactor: Transactor[F])
    extends TodoRepositoryAlgebra[F] {

  override def create(todo: Todo, userId: Long): F[Todo] =
    TodoSQL.insert(todo, userId)
      .withUniqueGeneratedKeys[Long]("ID")
      .map(id => todo.copy(id = id.some, userId = userId.some))
      .transact(transactor)

  override def update(todo: Todo): F[Option[Todo]] =
    OptionT
      .fromOption[ConnectionIO](todo.id)
      .semiflatMap(id => TodoSQL.update(todo, id).run.as(todo))
      .value
      .transact(transactor)

  override def get(id: Long): F[Option[Todo]] =
    TodoSQL.select(id).option
      .transact(transactor)

  override def delete(id: Long): F[Option[Todo]] =
    OptionT(TodoSQL.select(id).option)
      .semiflatMap(todo => TodoSQL.delete(id).run.as(todo))
      .value
      .transact(transactor)

  override def findByUser(userId: Long): F[List[Todo]] =
    TodoSQL.selectByUser(userId)
    .to[List]
    .transact(transactor)
}

object DoobieTodoRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieTodoRepositoryInterpreter[F] =
    new DoobieTodoRepositoryInterpreter(xa)
}
