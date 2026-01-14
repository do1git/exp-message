package site.rahoon.message.dbmigrations

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DbMigrationsApplication

fun main(args: Array<String>) {
	runApplication<DbMigrationsApplication>(*args)
}
