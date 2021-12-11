package io.github.valentun.todolist

import java.time.Instant
import cats.effect.IO
import domain.authentication.SignupRequest
import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary
import domain.todos._
import domain.todos.TodoPriority._
import domain.users.{Role, _}
import tsec.common.SecureRandomId
import tsec.jwt.JWTClaims
import tsec.authentication.AugmentedJWT
import tsec.jws.mac._
import tsec.mac.jca._

trait TodoListArbitraries {
  val userNameLength = 16
  val userNameGen: Gen[String] = Gen.listOfN(userNameLength, Gen.alphaChar).map(_.mkString)

  implicit val instant = Arbitrary[Instant] {
    for {
      millis <- Gen.posNum[Long]
    } yield Instant.ofEpochMilli(millis)
  }

  implicit val todoPriority = Arbitrary[TodoPriority] {
    Gen.oneOf(High, Medium, Low)
  }

  implicit val todo = Arbitrary[Todo] {
    for {
      name <- Gen.nonEmptyListOf(Gen.asciiPrintableChar).map(_.mkString)
      content <- arbitrary[String]
      priority <- arbitrary[TodoPriority]
    } yield Todo(
      name = name,
      content = content,
      priority = priority,
    )
  }

  implicit val role = Arbitrary[Role](Gen.oneOf(Role.values.toIndexedSeq))

  implicit val user = Arbitrary[User] {
    for {
      userName <- userNameGen
      email <- arbitrary[String]
      password <- arbitrary[String]
      role <- arbitrary[Role]
    } yield User(userName, email, password, role=role)
  }

  case class AdminUser(value: User)
  case class ClientUser(value: User)

  implicit val adminUser: Arbitrary[AdminUser] = Arbitrary {
    user.arbitrary.map(user => AdminUser(user.copy(role = Role.Admin)))
  }

  implicit val customerUser: Arbitrary[ClientUser] = Arbitrary {
    user.arbitrary.map(user => ClientUser(user.copy(role = Role.Client)))
  }

  implicit val userSignup = Arbitrary[SignupRequest] {
    for {
      userName <- userNameGen
      email <- arbitrary[String]
      password <- arbitrary[String]
      role <- arbitrary[Role]
    } yield SignupRequest(userName, email, password, role)
  }

  implicit val secureRandomId = Arbitrary[SecureRandomId] {
    arbitrary[String].map(SecureRandomId.apply)
  }

  implicit val jwtMac: Arbitrary[JWTMac[HMACSHA256]] = Arbitrary {
    for {
      key <- Gen.const(HMACSHA256.unsafeGenerateKey)
      claims <- Gen.finiteDuration.map(exp =>
        JWTClaims.withDuration[IO](expiration = Some(exp)).unsafeRunSync(),
      )
    } yield JWTMacImpure
      .build[HMACSHA256](claims, key)
      .getOrElse(throw new Exception("Inconceivable"))
  }

  implicit def augmentedJWT[A, I](implicit
      arb1: Arbitrary[JWTMac[A]],
      arb2: Arbitrary[I],
  ): Arbitrary[AugmentedJWT[A, I]] =
    Arbitrary {
      for {
        id <- arbitrary[SecureRandomId]
        jwt <- arb1.arbitrary
        identity <- arb2.arbitrary
        expiry <- arbitrary[Instant]
        lastTouched <- Gen.option(arbitrary[Instant])
      } yield AugmentedJWT(id, jwt, identity, expiry, lastTouched)
    }
}

object TodoListArbitraries extends TodoListArbitraries
