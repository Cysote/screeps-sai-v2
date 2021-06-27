package manager

import global.STORAGE_ID_TOKEN
import memory.sourceInfos
import memory.taskId
import memory.tasks
import screeps.api.*
import screeps.api.structures.Structure
import task.Task
import task.TaskType
import task.generator.EconomyTaskGenerator

class TaskManager {
    private val economyTaskGenerator = EconomyTaskGenerator()

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

    fun createTasksForRooms() {
        Game.rooms.values.forEach { room ->
            economyTaskGenerator.generateSourceHarvestTasks(room).let {
                Memory.tasks = Memory.tasks.plus(it)
            }

            economyTaskGenerator.generateBuildTask(room)?.let {
                Memory.tasks = Memory.tasks.plus(it)
            }
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

                if ((task.desiredWork <= 0 || provisionedWork < task.desiredWork)
                        && (task.desiredCarry <= 0 || provisionedCarry < task.desiredCarry)) {
                    tasksToReturn.add(task)
                }
            }
        }

        return tasksToReturn.sortedBy { TaskType.valueOf(it.type).priority }
    }

    /**
     * Read all tasks in the colony and update their targets, numbers, or other data as needed
     */
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    fun performTaskMaintenance() {
        Memory.tasks.forEach { task ->
            when (task.type) {

                /**
                 * HARVESTSOURCE maintenance steps:
                 * 1. Removing the deposit structure id when the structure is full
                 * 2. Updating the deposit structure id when it is blank
                 */
                TaskType.HARVESTSOURCE.name -> {
                    Game.rooms[task.owningRoom]?.let { room ->
                        room.memory.sourceInfos.find { info -> info.sourceId == task.targetId }?.let { srcInfo ->

                            // 1. Removing the deposit structure id when the structure is full
                            if (task.depositStructureId.isNotBlank()) {
                                Game.structures[task.depositStructureId]?.let { struct ->
                                    (struct as StoreOwner).let {
                                        if (struct.store.getFreeCapacity(RESOURCE_ENERGY) == 0) {
                                            task.depositStructureId = ""
                                        }
                                    }
                                }
                            }

                            // 2. Updating the deposit structure id when it is blank
                            if (task.depositStructureId.isBlank()) {
                                task.depositStructureId = when {
                                    srcInfo.sourceLinkId.isNotBlank() -> {
                                        srcInfo.sourceLinkId
                                    }
                                    srcInfo.sourceContainerId.isNotBlank() -> {
                                        srcInfo.sourceContainerId
                                    }
                                    room.storage != null -> {
                                        room.storage!!.id
                                    }
                                    else -> {
                                        val structToStoreIn = Game.structures.values.find { struct ->
                                            struct.room.name == room.name
                                                    && (struct.structureType == STRUCTURE_EXTENSION
                                                        || struct.structureType == STRUCTURE_SPAWN)
                                                    && (struct as StoreOwner).store.getFreeCapacity(RESOURCE_ENERGY) > 0
                                        }

                                        structToStoreIn?.id ?: ""
                                    }
                                }
                            }
                        }
                    }
                }

                /**
                 * BUILD maintenance steps:
                 * 1. Updating desired workers, work, and carry based on number of construction sites
                 * 2. Removing the withdraw structure id when the structure is empty
                 * 3. Updating the withdraw structure id when it is blank
                 */
                TaskType.BUILD.name -> {
                    Game.rooms[task.owningRoom]?.let { room ->

                        // TODO figure out a way to determine here whther to start a build task or not
                        // we don't want a build task if there are no energy containers in the room yet
                        val srcContainerNames = room.memory.sourceInfos.map { it.sourceContainerId }.toList()

                        if (srcContainerNames.isNotEmpty())
                        val allSites = Game.constructionSites.values.filter { site -> site.room?.name == task.owningRoom }

                        if (allSites.isEmpty()) {
                            task.isActive = false
                            task.desiredCreeps = 0
                            task.desiredWork = 0
                            task.desiredCarry = 0
                        } else {
                            var wantedWorkers = 0
                            var wantedWork = 0
                            var wantedCarry = 0
                            for (site in allSites) {
                                wantedWorkers += (site.progressTotal / 2500.0).toInt()
                                wantedWork += (site.progressTotal / 1500.0).toInt()
                                wantedCarry += (site.progressTotal / 750.0).toInt()
                            }

                            task.isActive = true
                            task.desiredCreeps = wantedWorkers
                            task.desiredWork = wantedWork
                            task.desiredCarry = wantedCarry

                            // 2. Removing the withdraw structure id when the structure is empty
                            if (task.withdrawStructureId.isNotBlank()) {
                                Game.structures[task.depositStructureId]?.let { struct ->
                                    (struct as StoreOwner).let {
                                        if (struct.store.getUsedCapacity(RESOURCE_ENERGY) < 25) {
                                            task.depositStructureId = ""
                                        }
                                    }
                                }
                            }

                            // 3. Updating the withdraw structure id when it is blank
                            if (task.withdrawStructureId.isNotBlank()) {
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

                // next
            }
        }
    }
}