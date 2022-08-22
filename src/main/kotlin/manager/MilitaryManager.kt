package manager

import global.MILITARY_FLAG_DIRECTIVES
import global.M_CLAIM_ROOM
import logger.logError
import memory.processed
import memory.tasks
import screeps.api.*
import task.TaskType
import task.createinfo.TaskToCreateInfo

/**
 * Manages spawning and controlling military creeps, and building room models for battles
 */
class MilitaryManager(private val myRooms: List<Room>) {
    private val militaryFlags: List<Flag> = Game.flags.values.filter { MILITARY_FLAG_DIRECTIVES.contains(it.name) }

    /**
     * Determines which of our owned rooms is closest to the target room
     */
    private fun determineClosestOwnedRoomTo(targetRoom: Room): Room? {
        var closestRoom: Room? = null
        var closestRoomPath: Array<Game.Map.RouteResult>? = null
        myRooms.forEach { room ->
            val findRouteResult = Game.map.findRoute(room.name, targetRoom.name)
            if (closestRoomPath == null
                    || (findRouteResult.value != null && findRouteResult.value!!.size > closestRoomPath!!.size)) {
                closestRoom = room
                closestRoomPath = findRouteResult.value!!
            }
        }

        return closestRoom
    }

    fun determineNewMilitaryTasks(): List<TaskToCreateInfo> {
        val newMilitaryTasks = mutableListOf<TaskToCreateInfo>()

        /**
         * Process flags to create the following tasks:
         * 1. Claim Room
         */
        militaryFlags.forEach { flag ->
            if (!flag.memory.processed) {
                when {

                    /**
                     * Create a new Claim task when
                     * 1. We can find a room close enough to the target
                     * 2. This room doesn't already have this task
                     */
                    flag.name.startsWith(M_CLAIM_ROOM) -> {
                        val targetRoom = Game.rooms[flag.pos.roomName]
                        if (targetRoom != null) {
                            val closestRoom = determineClosestOwnedRoomTo(targetRoom)
                            if (closestRoom == null)
                                logError("Failed to find an owned room closest to target room: $targetRoom")
                            else {
                                // TODO: Calculate distance between closest room and target room. Fail if a CLAIM creep
                                // would not be able to reach the room before dying (600 ticks?).
                                    // TODO: Maybe fold this logic into determineClosestOwnedRoomTo? Create new
                                        // parameter for max path search range
                                Memory.tasks.find { task ->
                                    task.type == TaskType.CLAIM.name
                                            && task.owningRoom == closestRoom.name
                                            && task.targetRoom == targetRoom.name
                                } ?: run {
                                    newMilitaryTasks.add(TaskToCreateInfo(
                                            owningRoom = closestRoom.name,
                                            taskType = TaskType.CLAIM.name,
                                            targetRoom = flag.pos.roomName
                                    ))
                                    flag.memory.processed = true
                                }
                            }
                        }
                    }
                }

                if (!flag.memory.processed) logError("Failed to parse military flag: ${flag.name}")
            }
        }

        return newMilitaryTasks
    }
}