package net.cakeyfox.foxy.database.utils

data class DatabaseConfig(
    val user: String,
    val password: String,
    val server: String,
    val database: String
) {
    class Builder {
        var user: String = ""
        var password: String = ""
        var server: String = ""
        var database: String = ""

        fun build(): DatabaseConfig {
            require(user.isNotEmpty()) { "Database user is required" }
            require(password.isNotEmpty()) { "Database password is required" }
            require(server.isNotEmpty()) { "Database server address is required" }
            require(database.isNotEmpty()) { "Database name is required" }

            return DatabaseConfig(user, password, server, database)
        }
    }
}