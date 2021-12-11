package io.github.valentun.todolist.config

final case class ServerConfig(host: String, port: Int)
final case class PetStoreConfig(db: DatabaseConfig, server: ServerConfig)
