package manager

import exception.InvalidIdException
import global.STORAGE_ID_TOKEN
import logger.logError
import memory.*
import screeps.api.*
import task.Task
import task.TaskType
import task.createinfo.TaskToCreateInfo
import task.generator.EconomyTaskGenerator
import task.generator.MilitaryTaskGenerator
import kotlin.math.ceil

/**
 * Generates tasks for rooms and performs maintenance on tasks to update
 * the values that determine the spawning and behavior of creeps
 */
class TaskManager {
    private val economyTaskGenerator = EconomyTaskGenerator()
    private val militaryTaskGenerator = MilitaryTaskGenerator()

    fun addCreepToTask(taskId: String, creep: Creep): Boolean {
        addCreepNameToTask(taskId, creep.name)
        creep.memory.taskId = taskId
        return true
    }

    fun addCreepNameToTask(taskId: String, creepName: String): Boolean {
        Memory.tasks.filter { it.id == taskId }[0].let {
            it.assignedCreeps = it.assignedCreeps.plus(creepName)
        }
        return true
    }

    fun createTasks(taskToCreateInfoList: List<TaskToCreateInfo>) {
        taskToCreateInfoList.forEach { taskToCreateInfo ->
            val room = Game.rooms[taskToCreateInfo.owningRoom]
            val taskType = TaskType.valueOf(taskToCreateInfo.taskType)
            val targetRoom = Game.rooms[taskToCreateInfo.targetRoom]

            if (room != null) {
                when (taskType.name) {

                    TaskType.HARVESTSOURCE.name -> {
                        economyTaskGenerator.generateSourceHarvestTasks(room).let {
                            Memory.tasks = Memory.tasks.plus(it)
                        }
                    }

                    TaskType.BUILD.name -> {
                        economyTaskGenerator.generateBuildTask(room).let {
                            Memory.tasks = Memory.tasks.plus(it)
                        }
                    }

                    TaskType.UPGRADE.name -> {
                        economyTaskGenerator.generateUpgradeTask(room).let {
                            Memory.tasks = Memory.tasks.plus(it)
                        }
                    }

                    TaskType.DELIVERY.name -> {
                        economyTaskGenerator.generateDeliveryTask(room).let {
                            Memory.tasks = Memory.tasks.plus(it)
                        }
                    }

                    TaskType.CLAIM.name -> {
                        if (targetRoom !== null) {
                            militaryTaskGenerator.generateClaimTask(room, targetRoom).let {
                                Memory.tasks = Memory.tasks.plus(it)
                            }
                        }
                    }
                }
            }
        }
    }

    /*fun createEconomicTasksForRooms(rooms: List<Room>) {
        rooms.forEach { room ->
            // Economy Tasks
            economyTaskGenerator.generateSourceHarvestTasks(room).let {
                Memory.tasks = Memory.tasks.plus(it)
            }

            economyTaskGenerator.generateBuildTask(room)?.let {
                Memory.tasks = Memory.tasks.plus(it)
            }

            economyTaskGenerator.generateUpgradeTask(room)?.let {
                Memory.tasks = Memory.tasks.plus(it)
            }

            economyTaskGenerator.generateDeliveryTask(room)?.let {
                Memory.tasks = Memory.tasks.plus(it)
            }

            // Military Tasks
            militaryTaskGenerator.generateClaimTask(room)?.let {
                Memory.tasks = Memory.tasks.plus(it)
            }
        }
    }*/

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

                if ((task.desiredWork <= 0 || provisionedWork < task.desiredWork)
                        && (task.desiredCarry <= 0 || provisionedCarry < task.desiredCarry)) {
                    tasksToReturn.add(task)
                }
            }
        }

        return tasksToReturn.sortedBy { TaskType.valueOf(it.type).priority }
    }

    /**
     * Read all tasks in the colony and update their targets, numbers, or other data as needed.
     * Some parts of task updates are gated by LOW_PRIORITY_TASK_UPDATE_TICKER_MAX
     */
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    fun performTaskMaintenance() {
        Memory.tasks.forEach { task ->
            when (task.type) {

                /**
                 * HARVESTSOURCE maintenance steps:
                 * 1. Remove the deposit structure id when the structure is full
                 * 2. Update the deposit structure id when it is blank
                 */
                TaskType.HARVESTSOURCE.name -> {
                    Game.rooms[task.owningRoom]?.let { room ->
                        room.memory.sourceInfos.find { info -> info.sourceId == task.targetId }?.let { srcInfo ->

                            // 1. Remove the deposit structure id when the structure is full
                            if (task.depositStructureId.isNotBlank()) {
                                Game.getObjectById<StoreOwner>(task.depositStructureId)?.let { struct ->
                                    struct.let {
                                        if (struct.store.getFreeCapacity(RESOURCE_ENERGY) == 0) {
                                            val structToStoreIn = Game.structures.values.find { struct ->
                                                struct.room.name == room.name
                                                        && (struct.structureType == STRUCTURE_EXTENSION
                                                            || struct.structureType == STRUCTURE_SPAWN)
                                                        && (struct as StoreOwner).store.getFreeCapacity(RESOURCE_ENERGY) > 0
                                            }
                                            task.depositStructureId = structToStoreIn?.id ?: ""
                                        }
                                    }
                                }
                            }

                            // 2. Update the deposit structure id when it is blank
                            if (task.depositStructureId.isBlank()) {
                                task.depositStructureId = when {
                                    srcInfo.sourceLinkId.isNotBlank() -> srcInfo.sourceLinkId
                                    srcInfo.sourceContainerId.isNotBlank() -> srcInfo.sourceContainerId
                                    else -> ""
                                }

                                if (task.depositStructureId.isBlank() && room.storage != null) task.depositStructureId = STORAGE_ID_TOKEN
                            }

                            if (Memory.lowPriorityTaskUpdateTicker == 0) {

                                // 3. Check that we have roads from at least one spawner
                                val spawners = Game.spawns.values.filter { it.room.name == room.name }
                                if (spawners.isNotEmpty()) {
                                    val taskSourceInfos = room.memory.sourceInfos.filter { it.sourceId == task.targetId }

                                    if (taskSourceInfos.size != 1) throw InvalidIdException("Expected only one source info to match task, but ${taskSourceInfos.size} did")

                                    val taskSourceInfo = taskSourceInfos[0]
                                    var bHasRoads = true
                                    posLoop@for (pos in taskSourceInfo.pathToSource) {
                                        val lookResult = room.lookForAt(LOOK_STRUCTURES, pos.x, pos.y)

                                        // Null means no buildings, including roads.
                                        if (lookResult == null) {
                                            bHasRoads = false
                                            break@posLoop
                                        } else {
                                            var bFoundARoad = false
                                            lookLoop@for (result in lookResult) {
                                                if (result.structureType == STRUCTURE_ROAD) {
                                                    bFoundARoad = true
                                                    break@lookLoop
                                                }
                                            }
                                            if (!bFoundARoad) {
                                                bHasRoads = false
                                                break@posLoop
                                            }
                                        }
                                    }

                                    task.bHasRoads = bHasRoads
                                }
                            }
                        }
                    }
                }

                /**
                 * BUILD maintenance steps:
                 * 1. Update desired workers, work, and carry based on number of construction sites
                 * 2. Remove the withdraw structure id when the structure is empty
                 * 3. Update the withdraw structure id when it is blank
                 */
                TaskType.BUILD.name -> {
                    Game.rooms[task.owningRoom]?.let { room ->

                        // Do not maintain build tasks until we see any kind of storage container in the room
                        val srcContainerNames = room.memory.sourceInfos.map { it.sourceContainerId }.toMutableList()
                        if (room.storage != null) srcContainerNames.add(STORAGE_ID_TOKEN)
                        if (srcContainerNames.isNotEmpty()) {
                            val allSitesInThisRoom = Game.constructionSites.values.filter { site -> site.room?.name == task.owningRoom }

                            if (allSitesInThisRoom.isEmpty()) {
                                task.isActive = false
                                task.desiredCreeps = 0
                                task.desiredWork = 0
                                task.desiredCarry = 0
                            }
                            // 1. Update desired workers, work, and carry based on number of construction sites
                            else {
                                var wantedWorkers = 0
                                var wantedWork = 0
                                var wantedCarry = 0
                                for (site in allSitesInThisRoom) {
                                    wantedWorkers += (site.progressTotal / 2500.0).toInt()
                                    wantedWork += (site.progressTotal / 1500.0).toInt()
                                    wantedCarry += (site.progressTotal / 750.0).toInt()
                                }

                                task.isActive = true
                                task.desiredCreeps = wantedWorkers.coerceAtMost(4)
                                task.desiredWork = wantedWork
                                task.desiredCarry = wantedCarry

                                // 2. Remove the withdraw structure id when the structure is empty
                                if (task.withdrawStructureId.isNotBlank()) {
                                    Game.getObjectById<StoreOwner>(task.withdrawStructureId)?.let { struct ->
                                        struct.let {
                                            if (struct.store.getUsedCapacity(RESOURCE_ENERGY) < 100) {
                                                task.withdrawStructureId = ""
                                            }
                                        }
                                    }
                                }

                                // 3. Update the withdraw structure id when it is blank
                                if (task.withdrawStructureId.isBlank()) {
                                    task.withdrawStructureId = if (room.storage != null) STORAGE_ID_TOKEN
                                    else {
                                        if (srcContainerNames.isNotEmpty()) {
                                            val srcContainers = srcContainerNames.map { Game.getObjectById<StoreOwner>(it) }.toList()
                                                    .sortedByDescending { it?.store?.getUsedCapacity(RESOURCE_ENERGY) }
                                            srcContainers[0]?.id ?: ""
                                        } else ""
                                    }
                                }
                            }
                        }
                    }
                }

                /**
                 * UPGRADE maintenance steps:
                 * 1. Update the withdraw structure id when it is blank
                 * 2. Remove the withdraw structure id when it doesn't exist
                 * 3. Fluctuate the desired work based on how much energy is in the container
                 */
                TaskType.UPGRADE.name -> {
                    Game.rooms[task.owningRoom]?.let { room ->

                        // 1. Update the controller structure id when it is blank
                        if (task.withdrawStructureId.isBlank()) {
                            task.isActive = false
                            if (room.memory.controllerInfo.controllerContainerId.isNotBlank()) {
                                task.withdrawStructureId = room.memory.controllerInfo.controllerContainerId
                            }
                        }

                        if (task.withdrawStructureId.isNotBlank()) {
                            val controllerContainer = Game.getObjectById<StoreOwner>(task.withdrawStructureId)

                            // 2. Remove the withdraw structure id when it doesn't exist
                            if (controllerContainer == null) task.withdrawStructureId = ""

                            // 3. Fluctuate the desired work based on how much energy is in the container
                            else {
                                task.isActive = true

                                task.desiredWork = if (room.storage != null) {
                                    val storedEnergy = room.storage!!.store[RESOURCE_ENERGY] ?: 0
                                    if (room.controller!!.level < 8) {
                                        (storedEnergy / 30000).coerceAtLeast(1)
                                    } else {
                                        // Don't go over 15, because at RCL 8, only 15 max (minus boosts) is counted towards GCL
                                        (storedEnergy / 30000).coerceIn(1, 15)
                                    }
                                } else {
                                    (ceil(controllerContainer.store.getUsedCapacity(RESOURCE_ENERGY)!! / 200.0).toInt()).coerceAtLeast(1)
                                }
                            }
                        }
                    }
                }

                /**
                 * DELIVERY maintenance steps:
                 * 1. Update desired workers and carry based on room energy and structures
                 * 2. Update the withdraw structure id when it is empty
                 */
                TaskType.DELIVERY.name -> {
                    Game.rooms[task.owningRoom]?.let { room ->
                        var bAllLinksActive = false

                        // 1. Update desired workers and carry based on room energy and structures

                        // If we have links for all sources, 1 delivery boi for every 11 room tasks
                        // to help handle when the room is managing many remote harvesting tasks
                        if (room.memory.sourceInfos.filter { it.sourceLinkId.isNotBlank() }.size == room.memory.sourceInfos.size) {
                            bAllLinksActive = true
                            task.desiredCreeps = ceil(Memory.tasks.filter { it.owningRoom == room.name }.size / 11.0).toInt()
                            task.desiredCarry = (room.energyCapacityAvailable / 75).coerceAtLeast(16)
                        }
                        // If we have source containers, calculate distance from controller for numbers
                        else if (room.memory.totalControllerDistance == 0 || task.desiredCarry <= 2) {
                            when {
                                room.memory.sourceInfos.filter { it.sourceContainerId.isNotBlank() }.size == room.memory.sourceInfos.size -> {
                                    var totalDistanceFromController = 0
                                    for (sourceInfo in room.memory.sourceInfos) {
                                        totalDistanceFromController += room.findPath(room.controller!!.pos, Game.getObjectById<Source>(sourceInfo.sourceId)!!.pos,
                                                options {
                                                    ignoreCreeps = true
                                                }).size
                                    }
                                    room.memory.totalControllerDistance = totalDistanceFromController

                                    // Times 2 because of going to and from sources
                                    val ticksToTransport = room.memory.totalControllerDistance * 2

                                    // TimeToTransport is 1:1 with tick rate. Average harvest rate is 1 tick to 10 energy
                                    val harvestableEnergyRelativeToTTT = ticksToTransport * 10

                                    // It takes time to fill extensions, so add half extension amount as padding
                                    val harvestableEnergyRelativeToTTTWithPadding = harvestableEnergyRelativeToTTT + (room.find(FIND_MY_STRUCTURES, options {
                                        filter = {
                                            it.structureType == STRUCTURE_EXTENSION
                                        }
                                    }).size / 2)

                                    // This is the amount of energy we need to use to create Delivery creeps to match harvest rate
                                    task.desiredCarry = ceil(harvestableEnergyRelativeToTTTWithPadding / 50.0).toInt().coerceAtLeast(10)

                                    // Chop into multiple pieces if it's really big
                                    val carryPerCreep = ceil((room.energyCapacityAvailable / (BODYPART_COST[CARRY]!! + BODYPART_COST[MOVE]!!)).toDouble())
                                    task.desiredCreeps = ceil(task.desiredCarry / carryPerCreep).toInt().coerceAtLeast(2)
                                }

                                // When we have our first two links, we should cut down
                                // on desired creeps when the creeps we can build are large
                                room.memory.routingStructuresInfo.linkId.isNotBlank() -> {
                                    task.desiredCreeps = 2
                                }

                                // We don't have all of our source containers yet, and maybe not all of our DELIVERY-reliant
                                // infrastructure. Keep the required carry amount low to allow things to be filled until we do.
                                else -> {
                                    task.desiredCreeps = 1
                                    task.desiredCarry = 2
                                }
                            }
                        }

                        // 2. Update the withdraw structure id when it is empty
                        if (bAllLinksActive && room.storage != null) {
                            task.withdrawStructureId = STORAGE_ID_TOKEN
                        } else {
                            if (task.withdrawStructureId.isNotBlank()) {
                                if (Game.getObjectById<StoreOwner>(task.withdrawStructureId)?.store?.getUsedCapacity(RESOURCE_ENERGY) < 100) {
                                    task.withdrawStructureId = ""
                                }
                            }

                            if (task.withdrawStructureId.isBlank()) {
                                task.withdrawStructureId = room.memory.sourceInfos
                                        .filter { it.sourceContainerId.isNotBlank() }
                                        .sortedByDescending { Game.getObjectById<StoreOwner>(it.sourceContainerId)?.store?.get(RESOURCE_ENERGY) }
                                        .getOrNull(0)?.sourceContainerId ?: ""
                            }
                        }
                    }
                }
            }
        }
    }
}