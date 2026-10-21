package edu.austral.splitit.server.application.port

interface TokenProvider {
    fun issue(user: AuthUser): String

    fun parse(token: String): AuthUser?

    fun issueGuest(guest: AuthGuest): String

    fun parseGuest(token: String): AuthGuest?
}
