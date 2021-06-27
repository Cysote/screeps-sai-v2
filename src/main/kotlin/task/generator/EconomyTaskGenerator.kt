package task.generator

import logger.logMessage
import memory.sourceInfos
import memory.tasks
import screeps.api.*
import task.Task
import task.TaskRole
import task.TaskType
import kotlin.math.floor
import kotlin.random.Random

class EconomyTaskGenerator {
    fun generateSourceHarvestTasks(room: Room): List<Task> {
        val numOfSources = room.memory.sourceInfos.size
        val harvestTasks = Memory.tasks.filter { task -> task.type == TaskType.HARVESTSOURCE.name && task.owningRoom == room.name }

        val tasks = mutableListOf<Task>()
        if (harvestTasks.size < numOfSources) {
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
                            desiredCreeps = maxEmptySpaces * 2,
                            desiredWork = 6,
                            desiredCarry = 0
                    )
                    tasks.add(harvestSourceTask)
                }
            }
        }
        return tasks
    }

    fun generateBuildTask(room: Room): Task? {
        Memory.tasks.find { task -> task.type == TaskType.BUILD.name && task.owningRoom == room.name }
                ?: run {
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
        return null
    }

    private fun generateTaskId(): String {
        var result = ""
        var j = 0

        while (j < 32) {
            if( j == 8 || j == 12|| j == 16|| j == 20) {
                result += "-"
            }
            val i = floor(Random.nextFloat()*16).toInt().toString(16).toUpperCase()
            result += i
            j++
        }

        return result
    }
}