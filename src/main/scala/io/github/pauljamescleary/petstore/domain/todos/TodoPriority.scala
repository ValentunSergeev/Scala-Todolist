package io.github.pauljamescleary.petstore.domain.todos

import enumeratum._

sealed trait TodoPriority extends EnumEntry

case object TodoPriority extends Enum[TodoPriority] with CirceEnum[TodoPriority] {

  val values = findValues

  case object High extends TodoPriority

  case object Medium extends TodoPriority

  case object Low extends TodoPriority
}


