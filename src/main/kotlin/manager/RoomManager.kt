package manager

import global.*
import info.ControllerInfo
import info.MineralInfo
import info.SourceInfo
import logger.logMessage
import logger.logPriorityMessage
import memory.*
import screeps.api.*
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureLink
import screeps.api.structures.StructureTower
import screeps.utils.unsafe.jsObject
import task.Task
import utils.Utils

/**
 * Manages all direct actions to rooms and structures in rooms across the entire colony.
 * Stores and manages room, source, controller, and mineral data
 */
class RoomManager {
    val myRooms = mutableListOf<Room>()

    init {
        Game.rooms.values.forEach { room ->
            if (room.controller != null && room.controller!!.my) {
                myRooms.add(room)
            }
        }
    }

    fun initRooms() {
        myRooms.forEach { room ->
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

                // Done, if we found everything. If we didn't find everything, we're probably in a sim room, so keep trying
                if (room.memory.sourceInfos.isNotEmpty() && room.memory.mineralInfos.isNotEmpty()) {
                    room.memory.initialized = true
                }
            }
        }
    }

    /**
     * Temp function for basic room defense. Should fold tower functionality into Military Manager for easier
     * coordination with active defense creeps, etc
     */
    fun activateTowers() {
        myRooms.forEach { room ->
            val hostileCreeps = room.find(FIND_HOSTILE_CREEPS)
            if (hostileCreeps.isNotEmpty()) {
                val roomTowers = Game.structures.values.filter { it.room.name == room.name && it.structureType == STRUCTURE_TOWER }
                roomTowers.forEach { tower ->
                    (tower as StructureTower).attack(hostileCreeps[0])
                }
            }
        }
    }

    fun levelUpRooms() {
        myRooms.forEach { room ->
            if (room.controller!!.level > room.memory.level) {
                room.memory.level += room.controller!!.level

                // Reset totalControllerDistance so that delivery tasks without links recalculate required delivery creeps
                room.memory.totalControllerDistance = 0

                // Create construction sites for new buildings
                val roomFlags = room.find(FIND_FLAGS)
                for (flag in roomFlags) {
                    when {
                        flag.name.startsWith(ROAD_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_ROAD)
                            flag.remove()
                        }
                        flag.name.startsWith(RAMPART_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_RAMPART)
                            flag.remove()
                        }
                        flag.name.startsWith(EXTENSION_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_EXTENSION)
                            flag.remove()
                        }
                        flag.name.startsWith(CONTAINER_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_CONTAINER)
                            flag.remove()
                        }
                        flag.name.startsWith(TOWER_TAG) -> {
                            if (room.controller!!.level >= 3) {
                                room.createConstructionSite(flag.pos, STRUCTURE_TOWER)
                                flag.remove()
                            }
                        }
                        flag.name.startsWith(STORAGE_TAG) -> {
                            if (room.controller!!.level >= 4) {
                                room.createConstructionSite(flag.pos, STRUCTURE_STORAGE)
                                flag.remove()
                            }
                        }
                        flag.name.startsWith(LINK_TAG) -> {
                            if (room.controller!!.level >= 5) {
                                room.createConstructionSite(flag.pos, STRUCTURE_LINK)
                                flag.remove()
                            }
                        }
                        flag.name.startsWith(EXTRACTOR_TAG) -> {
                            if (room.controller!!.level >= 6) {
                                room.createConstructionSite(flag.pos, STRUCTURE_EXTRACTOR)
                                flag.remove()
                            }
                        }
                        flag.name.startsWith(LAB_TAG) -> {
                            if (room.controller!!.level >= 6) {
                                room.createConstructionSite(flag.pos, STRUCTURE_STORAGE)
                                flag.remove()
                            }
                        }
                        flag.name.startsWith(TERMINAL_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_TERMINAL)
                            flag.remove()
                        }
                        flag.name.startsWith(FACTORY_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_FACTORY)
                            flag.remove()
                        }
                        flag.name.startsWith(SPAWN_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_SPAWN)
                            flag.remove()
                        }
                        flag.name.startsWith(OBSERVER_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_OBSERVER)
                            flag.remove()
                        }
                        flag.name.startsWith(POWER_SPAWN_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_POWER_SPAWN)
                            flag.remove()
                        }
                        flag.name.startsWith(NUKER_TAG) -> {
                            room.createConstructionSite(flag.pos, STRUCTURE_NUKER)
                            flag.remove()
                        }
                    }
                }
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

    fun lowPriorityRoomUpdates() {
        myRooms.forEach { room ->

            if (Memory.lowPriorityRoomUpdateTicker == 0) {

                /**
                 * Update source infos
                 */
                room.memory.sourceInfos.forEach { info ->
                    // We have a source link. Make sure it still exists
                    if (info.sourceLinkId.isNotBlank()) {
                        val sourceLink = Game.getObjectById<StructureLink>(info.sourceLinkId)
                        if (sourceLink == null) info.sourceLinkId = ""
                    }

                    // We don't have a source link. Look for structures at source and add link or container
                    else {
                        val source = Game.getObjectById<Source>(info.sourceId)!!
                        val lookResult = lookAtWithRange(source.pos, room, 2)

                        val lookFilteredForLink = lookResult.filter { it.type == LOOK_STRUCTURES  && it.structure!!.structureType == STRUCTURE_LINK }
                        if (lookFilteredForLink.isNotEmpty()) {
                            info.sourceLinkId = lookFilteredForLink[0].structure!!.id
                        } else {
                            if (info.sourceContainerId.isNotEmpty()) {
                                val sourceCont = Game.getObjectById<StructureContainer>(info.sourceContainerId)
                                if (sourceCont == null) info.sourceContainerId = ""
                            } else {
                                val lookFilteredForCont = lookResult.filter { it.type == LOOK_STRUCTURES  && it.structure!!.structureType == STRUCTURE_CONTAINER }
                                if (lookFilteredForCont.isNotEmpty()) {
                                    info.sourceContainerId = lookFilteredForCont[0].structure!!.id
                                }
                            }
                        }
                    }

                    // Calculate path to source if we don't have one
                    if (info.pathToSource.isEmpty()) {
                        val spawners = Game.spawns.values.filter { it.pos.roomName == room.name }
                        val source = Game.getObjectById<Source>(info.sourceId)!!
                        val searchResult = Utils.findPathToSource(spawners[0].pos, GoalWithRange(source.pos, 1))

                        if (!searchResult.incomplete) {
                            info.pathToSource = searchResult.path
                        }
                    }

                    // Build roads on path to source
                    if (info.pathToSource.isNotEmpty()) {
                        for (pos in info.pathToSource) {
                            room.createConstructionSite(RoomPosition(pos.x, pos.y, pos.roomName), STRUCTURE_ROAD)
                        }
                    }
                }

                /**
                 * Update controller info
                 */
                if (room.memory.controllerInfo.controllerContainerId.isNotBlank()) {
                    val cont = Game.getObjectById<StructureContainer>(room.memory.controllerInfo.controllerContainerId)
                    if (cont == null) room.memory.controllerInfo.controllerContainerId = ""
                }
                if (room.memory.controllerInfo.controllerContainerId.isBlank()) {
                    val lookResult = lookAtWithRange(room.controller!!.pos, room, 4)
                    val lookFilteredForCont = lookResult.filter { it.type == LOOK_STRUCTURES  && it.structure!!.structureType == STRUCTURE_CONTAINER }
                    if (lookFilteredForCont.isNotEmpty()) {
                        room.memory.controllerInfo.controllerContainerId = lookFilteredForCont[0].structure!!.id
                    }
                }
            }
        }
    }

    private fun lookAtWithRange(pos: RoomPosition, room: Room, range: Int): Array<Room.LookAtAreaArrayItem> {
        val top = (pos.y - range).coerceIn(0, 49)
        val left = (pos.x - range).coerceIn(0, 49)
        val bottom = (pos.y + range).coerceIn(0, 49)
        val right = (pos.x + range).coerceIn(0, 49)
        return room.lookAtAreaAsArray(top, left, bottom, right)
    }
}