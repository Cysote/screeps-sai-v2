package manager

import global.LOW_PRIORITY_ROOM_UPDATE_TICKER_MAX
import global.LOW_PRIORITY_TASK_UPDATE_TICKER_MAX
import global.M_CLAIM_ROOM
import logger.logMessage
import memory.*
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
    private val militaryManager = MilitaryManager(roomManager.myRooms)
    private val taskManager = TaskManager()

    // Tick
    init {
        if (Memory.lowPriorityRoomUpdateTicker > LOW_PRIORITY_ROOM_UPDATE_TICKER_MAX) Memory.lowPriorityRoomUpdateTicker = 0
        else Memory.lowPriorityRoomUpdateTicker = Memory.lowPriorityRoomUpdateTicker + 1

        if (Memory.lowPriorityTaskUpdateTicker > LOW_PRIORITY_TASK_UPDATE_TICKER_MAX) Memory.lowPriorityTaskUpdateTicker = 0
        else Memory.lowPriorityTaskUpdateTicker = Memory.lowPriorityTaskUpdateTicker + 1
    }

    /**
     * All colony actions are contained here
     */
    fun run() {
        initializeMemoryData()
        val deadCreeps: List<JsPair<String, CreepMemory>> = cleanupDeadCreepsMemory()
        removeDeadCreepsFromTasks(deadCreeps)
        resetSpawners()

        defendRooms()

        performTaskMaintenance()
        upgradeRooms()

        activateAliveCreeps()
        performCreepStateMaintenance()
        findNewTasks()

        val tasks = getActiveBelowCapacityTasks()
        assignCreepsToOrCreateCreepsForTasks(tasks)

        lowPriorityActions()

        // GIMME DEM PIXELS!
        if (Game.cpu.bucket == 10000) Game.cpu.generatePixel()
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

                // Ensure that at least one harvest source task and the carry task have at least one creep
                val criticalTask = determineCriticalTask(sortedTasks)
                if (criticalTask != null) {
                    fillTask(criticalTask, critical = true)
                }

                // Run this task loop for this room sorted by priority, and break out of it once we cannot fill a task
                taskLoop@for (task in sortedTasks) {
                    if (!fillTask(task)) break@taskLoop
                }
            }
        }
    }

    /**
     * Assign an idle creep or create a new creep for the given task
     * Return true if task was filled
     */
    private fun fillTask(task: Task, critical: Boolean = false): Boolean {
        val idleCreep = creepManager.getIdleCreepForTask(task)
        if (idleCreep != null)
            taskManager.addCreepToTask(task.id, idleCreep)
        else {
            val maxEnergy = if (critical) 300 else roomManager.getRoomMaxEnergy(task.owningRoom)
            val creepBody = creepManager.getCreepBodyForTask(task, maxEnergy)
            val createdCreepName = roomManager.createCreepForTask(task, creepBody)

            if (createdCreepName != null) taskManager.addCreepNameToTask(task.id, createdCreepName)
            else return false
        }
        return true
    }

    /**
     * Finds tasks that must be filled before all other tasks
     */
    private fun determineCriticalTask(tasks: List<Task>): Task? {
        val sourceTasks = tasks.filter { it.type == TaskType.HARVESTSOURCE.name }
        if (sourceTasks.isNotEmpty() && sourceTasks.all { it.assignedCreeps.isEmpty() }) {
            return sourceTasks[0]
        }

        val deliveryTasks = tasks.filter { it.type == TaskType.DELIVERY.name }
        if (deliveryTasks.isNotEmpty() && deliveryTasks.all { it.assignedCreeps.isEmpty() }) {
            return deliveryTasks[0]
        }

        return null
    }

    private fun performTaskMaintenance() {
        taskManager.performTaskMaintenance()
    }

    private fun upgradeRooms() {
        roomManager.levelUpRooms()
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
        val newTasks = roomManager.determineNewRoomTasks() +
                militaryManager.determineNewMilitaryTasks()

        taskManager.createTasks(newTasks)
    }

    /**
     * Creeps that are alive should try to perform their task if they have one
     * or they should try to get a task if they do not have one
     */
    private fun activateAliveCreeps() {
        creepManager.activateAliveCreeps()
    }

    /**
     * Creep state may change while performing their actions. Certain actions
     * may need to be performed based on those changes.
     */
    private fun performCreepStateMaintenance() {
        // Remove creeps from their assigned tasks if they are trying to give up their task
        Game.creeps.values.filter { it.memory.relinquishingTask }.forEach { creep ->
            taskManager.removeCreepFromTask(creep)
        }
    }

    /**
     * Potentially temp function. Should probably be handled by military manager
     */
    private fun defendRooms() {
        roomManager.activateTowers()
    }

    /**
     * Do the initial set up of memory data
     */
    private fun initializeMemoryData() {
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

    private fun lowPriorityActions() {
        roomManager.lowPriorityRoomUpdates()
        clearProcessedFlags()
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

    private fun clearProcessedFlags() {
        Game.flags.values.forEach { flag ->
            if (flag.memory.processed) {
                when {

                    /**
                     * Clear Claim flags when:
                     * 1. We own the room the claim flag is in
                     */
                    flag.name.startsWith(M_CLAIM_ROOM) -> {
                        val room = Game.rooms[flag.pos.roomName]
                        if (room != null) {
                            if (room.controller != null && room.controller!!.my)
                                flag.remove()
                        }
                    }
                }
            }
        }
    }
}
