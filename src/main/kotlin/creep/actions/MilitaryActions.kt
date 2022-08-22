package creep.actions

import annotations.ThrowsExceptions
import exception.MissingPrerequisitesException
import exception.NoRoomControllerException
import exception.TooManyPrerequisitesException
import global.M_CLAIM_ROOM
import logger.logError
import screeps.api.*

@ThrowsExceptions
open class MilitaryActions(private val creep: Creep) : MemoryActions(creep) {
    /**
     * Claim a room's controller for the colony
     */
    fun claimRoom(throwException: Boolean = true): Boolean {
        try {
            val foreignController = getForeignControllerFromTask()
            when (creep.claimController(foreignController)) {
                ERR_NOT_IN_RANGE -> creep.moveTo(foreignController)
            }
        } catch (e: RuntimeException) {
            when (e) {
                is NoRoomControllerException -> {
                    try {
                        val room = getTargetRoomFromTask()
                        val claimFlags = Game.flags.values.filter { it.pos.roomName == room.name && it.name.startsWith(M_CLAIM_ROOM)}
                        when {
                            claimFlags.isEmpty() -> {
                                val message = "Attempted to claim a room, but there isn't a claim flag on the room." +
                                        "TargetRoom: ${room.name} Creep: ${creep.name}"
                                logError(message)
                                throw MissingPrerequisitesException(message)
                            }
                            claimFlags.size > 1 -> {
                                val message = "Attempted to claim a room, but there is more than one claim flag on the room." +
                                        "TargetRoom: ${room.name} Creep: ${creep.name}"
                                logError(message)
                                throw TooManyPrerequisitesException(message)
                            }
                            else -> {
                                creep.moveTo(claimFlags[0])
                            }
                        }
                    } catch (e2: RuntimeException) {
                        if (throwException) throw e
                        else return false
                    }
                }
                else -> {
                    if (throwException) throw e
                    else return false
                }
            }
        }
        return true
    }
}