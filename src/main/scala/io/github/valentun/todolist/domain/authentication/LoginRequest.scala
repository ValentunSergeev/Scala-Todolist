package io.github.valentun.todolist
package domain.authentication

import domain.users.{Role, User}
import tsec.passwordhashers.PasswordHash

final case class LoginRequest(
    userName: String,
    password: String,
)

final case class SignupRequest(
    userName: String,
    email: String,
    password: String,
    role: Role,
) {
  def asUser[A](hashedPassword: PasswordHash[A]): User = User(
    userName,
    email,
    hashedPassword,
    role = role,
  )
}
