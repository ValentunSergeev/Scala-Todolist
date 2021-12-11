package io.github.pauljamescleary.petstore.domain.todos

trait TodoRepositoryAlgebra[F[_]] {
  def create(todo: Todo, userId: Long): F[Todo]

  def update(todo: Todo): F[Option[Todo]]

  def get(id: Long): F[Option[Todo]]

  def delete(id: Long): F[Option[Todo]]

  def findByUser(userId: Long): F[List[Todo]]
}
