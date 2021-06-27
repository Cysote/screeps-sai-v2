package manager

import info.ControllerInfo
import info.MineralInfo
import info.SourceInfo
import logger.logPriorityMessage
import memory.*
import screeps.api.*
import screeps.utils.unsafe.jsObject
import task.Task
import task.generator.EconomyTaskGenerator

/**
 * Manages all direct actions to rooms and structures in rooms across the entire colony.
 */
class RoomManager {
    private val economyTaskGenerator = EconomyTaskGenerator()

    fun initRooms() {
        Game.rooms.values.forEach { room ->
            if (!room.memory.initialized) {
                logPriorityMessage("Setting up room memory", room.name)

                room.memory.level = 0
                room.memory.sourceInfos = arrayOf()
                room.memory.mineralInfos = arrayOf()
                room.memory.controllerInfo = ControllerInfo()

                // Source Info
                val sources = room.find(FIND_SOURCES)
                var sourceInfos = arrayOf<SourceInfo>()
                for (source in sources) {
                    sourceInfos = sourceInfos.plus(SourceInfo(sourceId = source.id))
                }
                room.memory.sourceInfos = sourceInfos

                // Mineral Info
                val minerals = room.find(FIND_MINERALS)
                var mineralInfos = arrayOf<MineralInfo>()
                for (mineral in minerals) {
                    mineralInfos = mineralInfos.plus(MineralInfo(mineralId = mineral.id, mineralType = mineral.mineralType))
                }
                room.memory.mineralInfos = mineralInfos

                // Done
                room.memory.initialized = true
            }
        }
    }

    fun createCreepForTask(task: Task, creepBody: List<BodyPartConstant>): String? {
        Game.spawns.values.filter { it.room.name == task.owningRoom }.forEach { spawn ->
            if (!spawn.memory.preparingToSpawn && spawn.spawning == null) {
                val creepName = "E-${task.owningRoom}-${task.type}-${Game.time}"
                val spawnResult = spawn.spawnCreep(creepBody.toTypedArray(), creepName, options {
                    memory = jsObject<CreepMemory> {
                        this.taskId = task.id
                        this.reachedFullCapacity = false
                        this.owningRoom = task.owningRoom
                    }
                })

                if (spawnResult == OK) {
                    val work = creepBody.filter { it == WORK }.size
                    val carry = creepBody.filter { it == CARRY }.size
                    val claim = creepBody.filter { it == CLAIM }.size
                    val move = creepBody.filter { it == MOVE }.size
                    logPriorityMessage("Creating creep $creepName for task ${task.type} with id ${task.id} " +
                            "with body W$work-C$carry-CL$claim-M$move",
                            task.owningRoom)
                    spawn.memory.preparingToSpawn = true
                    return creepName
                }
            }
        }
        return null
    }

    fun getRoomMaxEnergy(roomName: String): Int {
        val room = Game.rooms[roomName] ?: return 0
        return room.energyCapacityAvailable
    }
}