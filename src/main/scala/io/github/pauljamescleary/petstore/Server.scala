package io.github.pauljamescleary.petstore

import cats.effect._
import doobie.util.ExecutionContexts
import io.circe.config.parser
import io.github.pauljamescleary.petstore.config._
import io.github.pauljamescleary.petstore.domain.authentication.Auth
import io.github.pauljamescleary.petstore.domain.todos._
import io.github.pauljamescleary.petstore.domain.users._
import io.github.pauljamescleary.petstore.infrastructure.endpoint._
import io.github.pauljamescleary.petstore.infrastructure.repository.doobie.{
  DoobieAuthRepositoryInterpreter,
  DoobieTodoRepositoryInterpreter,
  DoobieUserRepositoryInterpreter,
}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server => H4Server}
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)

  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, PetStoreConfig]("petstore"))
      serverEc <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      key <- Resource.eval(HMACSHA256.generateKey[F])
      authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)
      todoRepo = DoobieTodoRepositoryInterpreter[F](xa)
      todoValidation = TodoValidationInterpreter[F](todoRepo)
      userRepo = DoobieUserRepositoryInterpreter[F](xa)
      todoService = TodoService[F](todoRepo, todoValidation)
      userValidation = UserValidationInterpreter[F](userRepo)
      userService = UserService[F](userRepo, userValidation)
      authenticator = Auth.jwtAuthenticator[F, HMACSHA256](key, authRepo, userRepo)
      routeAuth = SecuredRequestHandler(authenticator)
      httpApp = Router(
        "/users" -> UserEndpoints
          .endpoints[F, BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[F], routeAuth),
        "/todos" -> TodoEndpoints.endpoints[F, HMACSHA256](todoService, routeAuth),
      ).orNotFound
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server
}
