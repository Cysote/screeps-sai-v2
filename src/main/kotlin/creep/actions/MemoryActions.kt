package creep.actions

import annotations.ThrowsExceptions
import exception.*
import global.STORAGE_ID_TOKEN
import logger.logMessage
import memory.*
import screeps.api.*
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureController
import task.Task

@ThrowsExceptions
open class MemoryActions(private val creep: Creep) {
    private val task: Task = getTaskFromCreepMemory(creep)

    fun updateCreepMemory() {
        if (creep.store.getFreeCapacity() == 0) {
            creep.memory.reachedFullCapacity = true
        }

        if (creep.store.getUsedCapacity() == 0) {
            creep.memory.reachedFullCapacity = false
        }
    }

    fun getTaskById(id: String): Task {
        val tasks: List<Task> = Memory.tasks.filter { task -> task.id == id }
        return when (tasks.size) {
            0 -> throw TaskNotFoundException("Cannot find task in global memory. Id: $id Creep: ${creep.name}")
            1 -> tasks.single()
            else -> throw MultipleMatchingTasksException("More than one task matched the id: $id")
        }
    }

    fun getTaskFromCreepMemory(creep: Creep): Task {
        return getTaskById(creep.memory.taskId)
    }

    fun getControllerFromTask(): StructureController {
        Game.rooms[task.owningRoom]?.let {
            if (it.controller != null) return it.controller!!
        }
        throw NoRoomControllerException("Room has no controller. Room: ${task.owningRoom} Task: ${task.id}")
    }

    fun getConstructionSiteByTaskRoom(): ConstructionSite {
        val roomSites = Game.constructionSites.values.filter { site -> site.room?.name == task.owningRoom }
        if (roomSites.isNotEmpty()) return roomSites[0]
        throw ConstructionSiteNotFoundException("No construction sites found in task room. Room: ${task.owningRoom} Task: ${task.id}")
    }

    fun getSourceFromTask(): Source {
        val s: Source? = Game.getObjectById<Source>(task.targetId)
        if (s != null) return s
        throw InvalidIdException("Attempted to get source from memory, but it doesn't exist. Creep: ${creep.name} Task: ${task.id}")
    }

    fun getSourceByTaskRoom(): Source {
        val s: Source? = Game.rooms[task.owningRoom]?.find(FIND_SOURCES)?.let {
            it[it.indices.random()]
        }
        if (s != null) return s
        throw NoRoomResourceException("Attempted to get source from task room, but could not find source. Creep: ${creep.name} Task: ${task.id}")
    }

    fun getDepositStructureFromTask(): StoreOwner {
        if (task.depositStructureId == STORAGE_ID_TOKEN) return Game.rooms[task.owningRoom]?.storage!!
        val s = Game.getObjectById<StoreOwner>(task.depositStructureId)
        if (s != null) return s
        throw InvalidIdException("Attempted to get deposit structure from memory, but it doesn't exist. Creep: ${creep.name} Task: ${task.id}")
    }

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    fun getDepositStructureByTaskRoom(): StoreOwner {
        val room = Game.rooms[task.owningRoom]!!

        // First, get spawning structs
        val notFullSpawningStructures = Game.structures.values.filter { struct ->
            struct.room.name == task.owningRoom
                    && (struct.structureType == STRUCTURE_SPAWN || struct.structureType == STRUCTURE_EXTENSION)
                    && (struct as StoreOwner).store.getFreeCapacity(RESOURCE_ENERGY) > 0
        }.toTypedArray()
        if (notFullSpawningStructures.isNotEmpty()) {
            return creep.pos.findClosestByRange(notFullSpawningStructures) as StoreOwner
        }

        // Next, get upgrade containers
        if (room.memory.controllerInfo.controllerContainerId.isNotBlank()) {
            val controllerCont = Game.getObjectById<StructureContainer>(room.memory.controllerInfo.controllerContainerId)
            if (controllerCont != null
                    && (controllerCont.store.getUsedCapacity(RESOURCE_ENERGY) < (2000*3)/4) ) {
                return controllerCont
            }
        }

        // Finally, get storage
        if (task.withdrawStructureId != STORAGE_ID_TOKEN && room.storage != null) {
            return room.storage!!
        }

        throw StructureNotFoundException("Could not find deposit structure in room: ${room.name}. Creep: ${creep.name} Task: ${task.id}")
    }

    fun getWithdrawStructureByTaskRoom(): StoreOwner {
        val room = Game.rooms[task.owningRoom]!!

        val sourceContainers = room.memory.sourceInfos
                .filter { it.sourceContainerId.isNotBlank() }
                .mapNotNull { Game.getObjectById<StructureContainer>(it.sourceContainerId) }
        if (sourceContainers.isNotEmpty()) {
            return sourceContainers.sortedByDescending { it.store.getUsedCapacity(RESOURCE_ENERGY) }[0]
        } else if (room.storage != null) return room.storage!!

        throw StructureNotFoundException("Could not find withdraw structure in room: ${room.name}. Creep: ${creep.name} Task: ${task.id}")
    }

    fun getWithdrawStructureFromTask(): StoreOwner {
        if (task.withdrawStructureId == STORAGE_ID_TOKEN) return Game.rooms[task.owningRoom]?.storage!!
        val s = Game.getObjectById<StoreOwner>(task.withdrawStructureId)
        if (s != null) return s
        throw InvalidIdException("Attempted to get withdraw structure from memory, but it doesn't exist. Creep: ${creep.name} Task: ${task.id}")
    }

    fun getWithdrawStructureByRoomController(): StoreOwner {
        Game.rooms[task.owningRoom]?.memory?.controllerInfo?.controllerContainerId?.let { sId ->
            val s = Game.getObjectById<StoreOwner>(sId)
            if (s != null) return s
        }
        throw InvalidIdException("Attempted to get withdraw structure from room controller info, but it doesn't exist. Creep: ${creep.name} Task: ${task.id}")
    }
}