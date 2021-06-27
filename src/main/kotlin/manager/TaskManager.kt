package manager

import memory.taskId
import memory.tasks
import screeps.api.*
import task.Task
import task.TaskType
import task.generator.EconomyTaskGenerator

class TaskManager {
    private val economyTaskGenerator = EconomyTaskGenerator()

    fun addCreepToTask(taskId: String, creep: Creep): Boolean {
        Memory.tasks.filter { it.id == taskId }[0].let {
            it.assignedCreeps = it.assignedCreeps.plus(creep.name)
        }
        creep.memory.taskId = taskId
        return true
    }

    fun createTasksForRooms() {
        Game.rooms.values.forEach { room ->
            val harvestTasks = economyTaskGenerator.generateSourceHarvestTasks(room)

            Memory.tasks = Memory.tasks.plus(harvestTasks)
        }
    }

    fun getActiveBelowCapacityTasks(): List<Task> {
        val tasksToReturn = mutableListOf<Task>()

        Memory.tasks.forEach { task ->
            if (task.isActive && (task.desiredCreeps <= 0 || task.assignedCreeps.size < task.desiredCreeps)) {

                var provisionedWork = 0
                var provisionedCarry = 0
                task.assignedCreeps.forEach { creepName ->
                    val creep = Game.creeps[creepName]
                    if (creep != null) {
                        provisionedWork += creep.body.filter { it.type == WORK }.size
                        provisionedCarry += creep.body.filter { it.type == CARRY }.size
                    }
                }

                if (provisionedWork < task.desiredWork && provisionedCarry < task.desiredCarry) {
                    tasksToReturn.add(task)
                }
            }
        }

        return tasksToReturn.sortedBy { TaskType.valueOf(it.type).priority }
    }
}