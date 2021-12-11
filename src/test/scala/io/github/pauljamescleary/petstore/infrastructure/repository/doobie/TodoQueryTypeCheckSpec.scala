package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import cats.effect.IO
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import io.github.pauljamescleary.petstore.TodoListArbitraries.todo
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TodoQueryTypeCheckSpec extends AnyFunSuite with Matchers with IOChecker {
  override val transactor: Transactor[IO] = testTransactor

  import TodoSQL._

  test("Typecheck todo queries") {
    todo.arbitrary.sample.foreach { todo =>
      check(insert(todo, 1))
      todo.id.foreach(id => check(update(todo, id)))
    }

    check(select(1L))
    check(delete(1L))
    check(selectByUser(1))
  }
}
