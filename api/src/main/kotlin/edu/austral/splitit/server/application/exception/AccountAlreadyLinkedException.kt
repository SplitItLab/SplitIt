package edu.austral.splitit.server.application.exception

class AccountAlreadyLinkedException(
    cause: Throwable? = null,
) : RuntimeException("Account is already linked to this event", cause)
