package edu.austral.splitit.server.domain.model.user

private const val MESSAGE = "Email cannot be changed on an account that signs in with Google"

class GoogleAccountEmailLockedException : RuntimeException(MESSAGE)
