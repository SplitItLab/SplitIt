package edu.austral.splitit.server.application.exception

class EventDeletionConflictException(
    message: String = "Event has related records",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
