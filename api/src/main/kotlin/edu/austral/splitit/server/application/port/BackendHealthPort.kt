package edu.austral.splitit.server.application.port

fun interface BackendHealthPort {
    fun status(): String
}
