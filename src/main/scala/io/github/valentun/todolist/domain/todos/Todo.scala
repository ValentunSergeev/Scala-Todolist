package io.github.valentun.todolist.domain.todos

case class Todo(
    name: String,
    content: String,
    priority: TodoPriority,
    id: Option[Long] = None,
    userId: Option[Long] = None,
)
