package edu.austral.splitit.server.infrastructure.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/")
class StatusController {
    companion object {
        const val STATUS = "OK"
    }

    @GetMapping("/status")
    fun status(): String = STATUS
}
