package edu.austral.splitit.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}

@RestController
class HelloController {
    @GetMapping("/")
    fun hello(): String {
        return "Hello, World!"
    }
}
