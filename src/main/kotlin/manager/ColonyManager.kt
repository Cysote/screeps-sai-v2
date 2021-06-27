package manager

import logger.logMessage
import logger.logPriorityMessage
import memory.preparingToSpawn
import memory.taskId
import memory.tasks
import screeps.api.*
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete
import task.Task
import task.TaskType

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
        val deadCreeps: List<JsPair<String, CreepMemory>> = cleanupDeadCreepsMemory()
        removeDeadCreepsFromTasks(deadCreeps)
        resetSpawners()

        performTaskMaintenance()

        activateAliveCreeps()
        findNewTasks()

        val tasks = getActiveBelowCapacityTasks()
        assignCreepsToOrCreateCreepsForTasks(tasks)


    }

    private fun assignCreepsToOrCreateCreepsForTasks(tasks: List<Task>) {
        val roomToTask = mutableMapOf<String, MutableList<Task>>()
        tasks.forEach { task ->
            val rttTask = roomToTask.getOrElse(task.owningRoom){ mutableListOf() }
            rttTask.add(task)
            roomToTask[task.owningRoom] = rttTask
        }

        roomToTask.keys.forEach { roomName ->
            roomToTask[roomName]?.sortedBy { TaskType.valueOf(it.type).priority }?.let { sortedTasks ->

                // Run this task loop for this room sorted by priority, and break out of it once we cannot fill a task
                taskLoop@for (task in sortedTasks) {
                    val idleCreep = creepManager.getIdleCreepForTask(task)
                    if (idleCreep != null)
                        taskManager.addCreepToTask(task.id, idleCreep)
                    else {
                        val creepBody = creepManager.getCreepBodyForTask(task, roomManager.getRoomMaxEnergy(task.owningRoom))
                        val createdCreepName = roomManager.createCreepForTask(task, creepBody)

                        if (createdCreepName != null) taskManager.addCreepNameToTask(task.id, createdCreepName)
                        else break@taskLoop
                    }
                }
            }
        }
    }

    private fun performTaskMaintenance() {
        taskManager.performTaskMaintenance()
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
    private fun cleanupDeadCreepsMemory(): List<JsPair<String, CreepMemory>> {
        if (Memory.creeps.isEmpty()) return listOf()

        val deadCreepEntries = mutableListOf<JsPair<String,CreepMemory>>()
        for (creepEntry in Memory.creeps) {
            if (Game.creeps[creepEntry.component1()] == null) {
                logMessage("Deleting dead creep ${creepEntry.component1()}")
                deadCreepEntries.add(creepEntry)
                delete(Memory.creeps[creepEntry.component1()])
            }
        }

        return deadCreepEntries
    }

    private fun removeDeadCreepsFromTasks(deadCreepEntries: List<JsPair<String, CreepMemory>>) {
        deadCreepEntries.forEach { entry ->
            val name = entry.component1()
            val memory = entry.component2()
            Memory.tasks.find { it.id == memory.taskId }?.let { task ->
                val assignedCreepsList = task.assignedCreeps.toMutableList()
                assignedCreepsList.remove(name)
                task.assignedCreeps = assignedCreepsList.toTypedArray()
            }
        }
    }
}