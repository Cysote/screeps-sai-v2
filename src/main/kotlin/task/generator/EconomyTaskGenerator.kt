package task.generator

import memory.sourceInfos
import memory.tasks
import screeps.api.*
import task.Task
import task.Task.Companion.generateTaskId
import task.TaskRole
import task.TaskType

/**
 * Creates economic tasks using given information. May also calculate
 * information needed to initialize defaults for the task.
 */
class EconomyTaskGenerator {
    fun generateSourceHarvestTasks(room: Room): List<Task> {
        val harvestTasks = Memory.tasks.filter { task -> task.type == TaskType.HARVESTSOURCE.name && task.owningRoom == room.name }
        val tasks = mutableListOf<Task>()
        for (info in room.memory.sourceInfos) {
            if (harvestTasks.find { task -> task.targetId == info.sourceId } == null) {

                // Calculate how many workers can fit on the source (empty spaces next to source * 2)
                val source = Game.getObjectById<Source>(info.sourceId)!!
                val lookResult = room.lookAtAreaAsArray(source.pos.y-1, source.pos.x-1, source.pos.y+1, source.pos.x+1)
                val maxEmptySpaces = lookResult.filter { space -> space.type == LOOK_TERRAIN && space.terrain != TERRAIN_WALL }.size

                val harvestSourceTask = Task(
                        id = generateTaskId(),
                        role = TaskRole.ECONOMY.name,
                        type = TaskType.HARVESTSOURCE.name,
                        isActive = true,
                        owningRoom = room.name,
                        targetId = info.sourceId,
                        depositStructureId = "",
                        desiredCreeps = maxEmptySpaces,
                        desiredWork = 6,
                        desiredCarry = 0
                )
                tasks.add(harvestSourceTask)
            }
        }
        return tasks
    }

    fun generateBuildTask(room: Room): Task {
        // No initialization. The task is dynamically modified in Task Manager as construction sites are found
        return Task(
                id = generateTaskId(),
                role = TaskRole.ECONOMY.name,
                type = TaskType.BUILD.name,
                isActive = true,
                owningRoom = room.name,
                targetId = "",
                withdrawStructureId = "",
                desiredCreeps = 0,
                desiredWork = 0,
                desiredCarry = 0
        )
    }

    fun generateUpgradeTask(room: Room): Task {
        // No initialization. The task is dynamically modified in Task Manager as the controller container energy fluxes
        return Task(
                id = generateTaskId(),
                role = TaskRole.ECONOMY.name,
                type = TaskType.UPGRADE.name,
                isActive = true,
                owningRoom = room.name,
                targetId = "",
                withdrawStructureId = "",
                desiredCreeps = 0,
                desiredWork = 0,
                desiredCarry = 0
        )
    }

    fun generateDeliveryTask(room: Room): Task {
        // No initialization. The task is dynamically modified in Task Manager as room energy fluxes
        return Task(
                id = generateTaskId(),
                role = TaskRole.ECONOMY.name,
                type = TaskType.DELIVERY.name,
                isActive = true,
                owningRoom = room.name,
                targetId = "",
                withdrawStructureId = "",
                desiredCreeps = 0,
                desiredWork = 0,
                desiredCarry = 0
        )
    }
}