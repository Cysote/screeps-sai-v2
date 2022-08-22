package task.generator

import screeps.api.Room
import task.Task
import task.TaskRole
import task.TaskType

class MilitaryTaskGenerator {
    fun generateClaimTask(room: Room, targetRoom: Room): Task {
        return Task(
                id = Task.generateTaskId(),
                role = TaskRole.MILITARY.name,
                type = TaskType.CLAIM.name,
                isActive = true,
                owningRoom = room.name,
                targetRoom = targetRoom.name,
                targetId = "",
                withdrawStructureId = "",
                desiredCreeps = 0,
                desiredWork = 0,
                desiredCarry = 0
        )
    }
}