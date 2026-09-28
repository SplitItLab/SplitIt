package edu.austral.splitit.server.application.service

import edu.austral.splitit.server.application.exception.EventDeletionConflictException
import edu.austral.splitit.server.application.exception.EventNotFoundException
import edu.austral.splitit.server.domain.model.event.Currency
import edu.austral.splitit.server.domain.service.EventMemberService
import edu.austral.splitit.server.domain.service.EventService
import edu.austral.splitit.server.domain.service.ExpenseService
import edu.austral.splitit.server.domain.service.InviteLinkService
import edu.austral.splitit.server.domain.service.UserService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

data class CreateEventCommand(
    val userId: Long,
    val name: String,
    val description: String? = null,
    val iconKey: String? = null,
    val baseCurrency: String,
    val participantNames: List<String> = emptyList(),
)

data class UpdateEventCommand(
    val userId: Long,
    val eventId: Long,
    val name: String? = null,
    val description: String? = null,
    val iconKey: String? = null,
)

data class EventSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val baseCurrency: String,
    val memberCount: Long,
)

data class EventDetail(
    val id: Long,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val baseCurrency: String,
    val memberCount: Long,
    val members: List<EventMemberSummary>,
    val isOwner: Boolean,
)

data class EventMemberSummary(
    val id: Long,
    val name: String,
    val email: String?,
    val isGuest: Boolean,
)

data class CreateExpenseCommand(
    val userId: Long,
    val eventId: Long,
    val name: String,
    val amount: BigDecimal,
    val currency: String,
    val paidByMemberId: Long,
)

data class ExpensePayerSummary(
    val id: Long,
    val name: String,
)

data class ExpenseSummary(
    val id: Long,
    val eventId: Long,
    val name: String,
    val originalAmount: BigDecimal,
    val originalCurrency: String,
    val baseAmount: BigDecimal,
    val baseCurrency: String,
    val paidByMember: ExpensePayerSummary,
    val expenseDate: LocalDate,
)

@Service
class EventApplicationService(
    private val userService: UserService,
    private val eventService: EventService,
    private val eventMemberService: EventMemberService,
    private val inviteLinkService: InviteLinkService,
    private val expenseService: ExpenseService,
) {
    @Transactional
    fun createEvent(command: CreateEventCommand): EventSummary {
        val owner = userService.getById(command.userId)

        val normalizedAdditionalNames =
            command.participantNames.map { name ->
                val trimmed = name.trim()
                require(trimmed.isNotEmpty()) {
                    "Participant name cannot be empty"
                }
                trimmed
            }

        val ownerKey = owner.name.trim().lowercase()
        val additionalKeys = normalizedAdditionalNames.map { it.lowercase() }
        require(additionalKeys.toSet().size == additionalKeys.size) {
            "Duplicate participant names"
        }
        require(ownerKey !in additionalKeys) {
            "Participant name cannot match the event owner name"
        }

        val event =
            eventService.save(
                owner = owner,
                name = command.name,
                description = command.description,
                iconKey = command.iconKey,
                baseCurrency = command.baseCurrency,
            )

        eventMemberService.addMember(
            event = event,
            displayName = owner.name,
            user = owner,
        )

        if (normalizedAdditionalNames.isNotEmpty()) {
            eventMemberService.addMembers(event, normalizedAdditionalNames)
        }

        val totalMembers = 1L + normalizedAdditionalNames.size

        return EventSummary(
            id = requireNotNull(event.id),
            name = event.name,
            description = event.description,
            iconKey = event.iconKey,
            baseCurrency = event.baseCurrency,
            memberCount = totalMembers,
        )
    }

    @Transactional(readOnly = true)
    fun listUserEvents(userId: Long): List<EventSummary> {
        val events = eventService.findUserEvents(userId)
        if (events.isEmpty()) return emptyList()

        val eventIds = events.mapNotNull { it.id }
        val counts = eventMemberService.getMemberCounts(eventIds)

        return events.map { event ->
            EventSummary(
                id = requireNotNull(event.id),
                name = event.name,
                description = event.description,
                iconKey = event.iconKey,
                baseCurrency = event.baseCurrency,
                memberCount = counts[event.id] ?: 0L,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getEventById(
        userId: Long,
        eventId: Long,
    ): EventDetail {
        val event = eventService.findById(eventId) ?: throw EventNotFoundException()

        val isOwner = event.owner.id == userId
        val isMember = isOwner || eventMemberService.isUserMemberOfEvent(eventId, userId)

        if (!isMember) {
            throw AccessDeniedException("Forbidden")
        }

        val members = eventMemberService.findByEventId(eventId)

        return EventDetail(
            id = requireNotNull(event.id),
            name = event.name,
            description = event.description,
            iconKey = event.iconKey,
            baseCurrency = event.baseCurrency,
            memberCount = members.size.toLong(),
            members =
                members.map { member ->
                    EventMemberSummary(
                        id = requireNotNull(member.id),
                        name = member.displayName,
                        email = member.user?.email,
                        isGuest = member.user == null,
                    )
                },
            isOwner = isOwner,
        )
    }

    @Transactional
    fun getOrCreateInviteToken(
        userId: Long,
        eventId: Long,
    ): String {
        val event = eventService.findById(eventId) ?: throw EventNotFoundException()

        if (event.owner.id != userId) {
            throw AccessDeniedException("Forbidden")
        }

        return inviteLinkService.getOrCreate(event).token
    }

    @Transactional
    fun addExpense(command: CreateExpenseCommand): ExpenseSummary {
        val event = eventService.findById(command.eventId) ?: throw EventNotFoundException()

        val isOwner = event.owner.id == command.userId
        val isMember = isOwner || eventMemberService.isUserMemberOfEvent(command.eventId, command.userId)
        if (!isMember) {
            throw AccessDeniedException("Forbidden")
        }

        val paidByMember = eventMemberService.findById(command.paidByMemberId)
        require(paidByMember != null && paidByMember.event.id == event.id) {
            "Paying member must belong to the requested event"
        }

        require(Currency(command.currency).get() == event.baseCurrency) {
            "Expense currency must match the event base currency"
        }

        val expense =
            expenseService.addExpense(
                event = event,
                paidByMember = paidByMember,
                name = command.name,
                amount = command.amount,
                currency = command.currency,
            )

        return ExpenseSummary(
            id = requireNotNull(expense.id),
            eventId = requireNotNull(event.id),
            name = expense.name,
            originalAmount = expense.originalAmount,
            originalCurrency = expense.originalCurrency,
            baseAmount = expense.baseAmount,
            baseCurrency = event.baseCurrency,
            paidByMember =
                ExpensePayerSummary(
                    id = requireNotNull(paidByMember.id),
                    name = paidByMember.displayName,
                ),
            expenseDate = expense.expenseDate,
        )
    }

    @Transactional(readOnly = true)
    fun listExpenses(
        userId: Long,
        eventId: Long,
    ): List<ExpenseSummary> {
        val event = eventService.findById(eventId) ?: throw EventNotFoundException()

        val isOwner = event.owner.id == userId
        val isMember = isOwner || eventMemberService.isUserMemberOfEvent(eventId, userId)
        if (!isMember) {
            throw AccessDeniedException("Forbidden")
        }

        return expenseService.findByEventId(eventId).map { expense ->
            ExpenseSummary(
                id = requireNotNull(expense.id),
                eventId = requireNotNull(event.id),
                name = expense.name,
                originalAmount = expense.originalAmount,
                originalCurrency = expense.originalCurrency,
                baseAmount = expense.baseAmount,
                baseCurrency = event.baseCurrency,
                paidByMember =
                    ExpensePayerSummary(
                        id = requireNotNull(expense.paidByMember.id),
                        name = expense.paidByMember.displayName,
                    ),
                expenseDate = expense.expenseDate,
            )
        }
    }

    @Transactional
    fun updateEvent(command: UpdateEventCommand): EventSummary {
        val event =
            eventService.findById(command.eventId)
                ?: throw EventNotFoundException()

        require(event.owner.id == command.userId) {
            throw AccessDeniedException("Forbidden")
        }

        val updated =
            eventService.update(
                event = event,
                name = command.name,
                description = command.description,
                iconKey = command.iconKey,
            )

        val members = eventMemberService.findByEventId(command.eventId)

        return EventSummary(
            id = requireNotNull(updated.id),
            name = updated.name,
            description = updated.description,
            iconKey = updated.iconKey,
            baseCurrency = updated.baseCurrency,
            memberCount = members.size.toLong(),
        )
    }

    @Transactional
    fun deleteEvent(
        userId: Long,
        eventId: Long,
    ) {
        val event = eventService.getById(eventId)

        require(event.owner.id == userId) {
            throw AccessDeniedException("Forbidden")
        }

        ensureDeletable(eventId)

        try {
            eventMemberService.deleteByEventId(eventId)
            eventService.delete(event)
        } catch (exception: DataIntegrityViolationException) {
            throw EventDeletionConflictException(cause = exception)
        }
    }

    private fun ensureDeletable(eventId: Long) {
        if (eventService.hasExpenses(eventId)) {
            throw EventDeletionConflictException()
        }
    }
}
