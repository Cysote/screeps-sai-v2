package manager

import logger.logMessage
import memory.preparingToSpawn
import screeps.api.*
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete
import task.Task

/**
 * Entry point for all colony functions.
 * Describes the high level flow of colony actions taken.
 */
class ColonyManager {
    private val roomManager = RoomManager()
    private val creepManager = CreepManager()
    private val taskManager = TaskManager()

    fun run() {
        initializeData()
        cleanupDeadCreepsMemory()
        resetSpawners()

        activateAliveCreeps()
        findNewTasks()

        val tasks = getActiveBelowCapacityTasks()
        assignCreepsToOrCreateCreepsForTasks(tasks)


    }

    private fun assignCreepsToOrCreateCreepsForTasks(tasks: List<Task>) {
        tasks.forEach { task ->
            val idleCreep = creepManager.getIdleCreepForTask(task)
            if (idleCreep != null)
                taskManager.addCreepToTask(task.id, idleCreep)
            else { logMessage("2")
                val creepBody = creepManager.getCreepBodyForTask(task, roomManager.getRoomMaxEnergy(task.owningRoom))
                roomManager.createCreepForTask(task, creepBody)
            }
        }
    }

    private fun resetSpawners() {
        Game.spawns.values.forEach { spawn ->
            spawn.memory.preparingToSpawn = false
        }
    }

    private fun getActiveBelowCapacityTasks(): List<Task> {
        return taskManager.getActiveBelowCapacityTasks()
    }

    /**
     * Create new tasks for creeps to perform and add them to the global memory task list
     */
    private fun findNewTasks() {
        taskManager.createTasksForRooms()
    }

    /**
     * Creeps that are alive should try to perform their task if they have one
     * or they should try to get a task if they do not have one
     */
    private fun activateAliveCreeps() {
        creepManager.activateAliveCreeps()
    }

    /**
     * Do the initial set up of room memory
     */
    private fun initializeData() {
        roomManager.initRooms()
    }

    /**
     * Removes dead creeps from game memory
     */
    private fun cleanupDeadCreepsMemory(): List<String> {
        if (Memory.creeps.isEmpty()) return listOf()

        val deadCreepNames = mutableListOf<String>()
        for ((name, _) in Memory.creeps) {
            if (Game.creeps[name] == null) {
                logMessage("Deleting dead creep $name")
                deadCreepNames.add(name)
                delete(Memory.creeps[name])
            }
        }

        return deadCreepNames
    }
}