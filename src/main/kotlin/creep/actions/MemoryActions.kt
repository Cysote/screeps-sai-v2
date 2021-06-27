package creep.actions

import annotations.ThrowsExceptions
import exception.IdInvalidException
import exception.MultipleMatchingTasksException
import exception.TaskNotFoundException
import memory.reachedFullCapacity
import memory.taskId
import memory.tasks
import screeps.api.*
import task.Task

@ThrowsExceptions
open class MemoryActions(private val creep: Creep) {
    val task: Task = getTaskFromCreepMemory(creep)

    fun updateCreepMemory() {
        if (creep.store.getFreeCapacity() == 0) {
            creep.memory.reachedFullCapacity = true
        }
    }

    fun getTaskById(id: String): Task {
        val tasks: List<Task> = Memory.tasks.filter { task -> task.id == id }
        return when (tasks.size) {
            0 -> throw TaskNotFoundException("Cannot find task in global memory. Id: $id")
            1 -> tasks.single()
            else -> throw MultipleMatchingTasksException("More than one task matched the id: $id")
        }
    }

    fun getTaskFromCreepMemory(creep: Creep): Task {
        return getTaskById(creep.memory.taskId)
    }

    fun getSourceFromTask(): Source {
        val s: Source? = Game.getObjectById<Source>(task.targetId)
        if (s != null) return s
        throw IdInvalidException("Attempted to get source from memory, but it doesn't exist. Creep: ${creep.name} Task: ${task.targetId}")
    }

    fun getDepositStructureFromTask(): StoreOwner {
        val s = Game.getObjectById<StoreOwner>(task.depositStructureId)
        if (s != null) return s
        throw IdInvalidException("Attempted to get deposit structure from memory, but it doesn't exist. Creep: ${creep.name} Task: ${task.targetId}")
    }
}