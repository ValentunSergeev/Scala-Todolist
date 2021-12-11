package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.effect._
import io.circe.generic.auto._
import io.github.pauljamescleary.petstore.domain.todos._
import io.github.pauljamescleary.petstore.domain.users._
import io.github.pauljamescleary.petstore.infrastructure.repository.inmemory._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.server.Router
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.mac.jca.HMACSHA256

class TodoEndpointsSpec
    extends AnyFunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with TodoListArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {
  implicit val todoEnc: EntityEncoder[IO, Todo] = jsonEncoderOf
  implicit val todoDec: EntityDecoder[IO, Todo] = jsonOf
  implicit val todoListEnc: EntityDecoder[IO, List[Todo]] = jsonOf

  def getTestResources(): (AuthTest[IO], HttpApp[IO], TodoRepositoryInMemoryInterpreter[IO]) = {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val todoRepo = TodoRepositoryInMemoryInterpreter[IO]()
    val todoValidation = TodoValidationInterpreter[IO](todoRepo)
    val todoService = TodoService[IO](todoRepo, todoValidation)
    val auth = new AuthTest[IO](userRepo)
    val todoEndpoint = TodoEndpoints.endpoints[IO, HMACSHA256](todoService, auth.securedRqHandler)
    val todoRoutes = Router(("/todos", todoEndpoint)).orNotFound
    (auth, todoRoutes, todoRepo)
  }

  test("create todo") {
    val (auth, todoRoutes, _) = getTestResources()

    forAll { (todo: Todo, user: User) =>
      (for {
        savedUser <- auth.insertUser(user)
        createRq <- POST(todo, uri"/todos")
          .flatMap(auth.embedToken(savedUser, _))
        response <- todoRoutes.run(createRq)
        createdTodo <- response.as[Todo]
        userTodosReq <- GET(Uri.unsafeFromString(s"/todos"))
          .flatMap(auth.embedToken(savedUser, _))
        response2 <- todoRoutes.run(userTodosReq)
        response2Decoded <- response2.as[List[Todo]]
      } yield {
        response2Decoded.size shouldEqual 1
        response2Decoded.head.id shouldEqual createdTodo.id
      }).unsafeRunSync()
    }

    forAll { todo: Todo =>
      (for {
        request <- POST(todo, uri"/todos")
        response <- todoRoutes.run(request)
      } yield response.status shouldEqual Unauthorized).unsafeRunSync()
    }

    forAll { (todo: Todo, user: User) =>
      (for {
        request <- POST(todo, uri"/todos")
          .flatMap(auth.createUserAndEmbedToken(user, _))
        response <- todoRoutes.run(request)
      } yield response.status shouldEqual Ok).unsafeRunSync()
    }
  }

  test("update todo") {
    val (auth, todoRoutes, _) = getTestResources()

    forAll { (todo: Todo, user: User) =>
      (for {
        savedUser <- auth.insertUser(user)
        createRequest <- POST(todo, uri"/todos")
          .flatMap(auth.embedToken(savedUser, _))
        createResponse <- todoRoutes.run(createRequest)
        createdTodo <- createResponse.as[Todo]
        todoToUpdate = createdTodo.copy(name = createdTodo.name.reverse)
        updateRequest <- PUT(todoToUpdate, Uri.unsafeFromString(s"/todos/${todoToUpdate.id.get}"))
          .flatMap(auth.embedToken(savedUser, _))
        updateResponse <- todoRoutes.run(updateRequest)
        updatedTodo <- updateResponse.as[Todo]
      } yield updatedTodo.name shouldEqual todo.name.reverse).unsafeRunSync()
    }
  }
}
