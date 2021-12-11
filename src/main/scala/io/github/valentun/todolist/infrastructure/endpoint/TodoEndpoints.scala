package io.github.valentun.todolist.infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.valentun.todolist.domain.authentication.Auth
import io.github.valentun.todolist.domain.todos.{Todo, TodoNotFoundError, TodoService, WrongUserError}
import io.github.valentun.todolist.domain.users.User
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class TodoEndpoints[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  implicit val todoDecoder: EntityDecoder[F, Todo] = jsonOf[F, Todo]

  def endpoints(
      todoService: TodoService[F],
      auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val allRoles = createTodoEndpoint(todoService)
        .orElse(updateTodoEndpoint(todoService))
        .orElse(getUserTodosEndpoint(todoService))
        .orElse(deleteTodoEndpoint(todoService))

      Auth.allRoles(allRoles)
    }

    auth.liftService(authEndpoints)
  }

  private def createTodoEndpoint(todoService: TodoService[F]): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed user =>
      for {
        todo <- req.request.as[Todo]
        saved <- todoService.create(todo, user.id.get)
        resp <- Ok(saved.asJson)
      } yield resp
  }

  private def updateTodoEndpoint(todoService: TodoService[F]): AuthEndpoint[F, Auth] = {
    case req @ PUT -> Root / LongVar(_) asAuthed user =>
      val action = for {
        todo <- req.request.as[Todo]
        result <- todoService.update(todo, user.id.get).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(TodoNotFoundError) => NotFound("The todo was not found")
        case Left(WrongUserError) => Forbidden("You are not owner of this TODO")
      }
  }

  private def getUserTodosEndpoint(todoService: TodoService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root asAuthed user =>
      for {
        todos <- todoService.findByUser(user.id.get)
        result <- Ok(todos.asJson)
      } yield result
  }

  private def deleteTodoEndpoint(todoService: TodoService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- todoService.delete(id)
        resp <- Ok()
      } yield resp
  }
}

object TodoEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](
      todoService: TodoService[F],
      auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] =
    new TodoEndpoints[F, Auth].endpoints(todoService, auth)
}
