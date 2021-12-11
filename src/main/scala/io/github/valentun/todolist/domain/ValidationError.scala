package io.github.valentun.todolist.domain

import users.User

sealed trait ValidationError extends Product with Serializable
sealed trait UserError extends ValidationError
case object UserNotFoundError extends UserError
case object WrongUserError extends UserError
case class UserAlreadyExistsError(user: User) extends ValidationError
case class UserAuthenticationFailedError(userName: String) extends ValidationError
