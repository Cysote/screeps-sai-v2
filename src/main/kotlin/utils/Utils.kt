package utils

import screeps.api.*

class Utils() {
    companion object {

        /**
         * Source Paths should
         * Favor:
         *     Roads
         * Ignore:
         *     Creeps
         *     Containers
         *     Ramparts
         * Avoid:
         *     All other structures
         */
        fun findPathToSource(pos: RoomPosition, range: GoalWithRange): PathFinder.Path {
            return PathFinder.search(pos, range, options {
                plainCost = 2
                swampCost = 10

                roomCallback = {
                    val cm = PathFinder.CostMatrix()

                    Game.rooms[pos.roomName]?.let { room ->
                        val roomStructures = room.find(FIND_STRUCTURES)
                        if (roomStructures.isNotEmpty()) {
                            for (struct in roomStructures) {
                                if (struct.structureType == STRUCTURE_ROAD) {
                                    cm.set(struct.pos.x, struct.pos.y, 1)
                                } else if (struct.structureType == STRUCTURE_RAMPART
                                        && struct.structureType == STRUCTURE_CONTAINER) {
                                    cm.set(struct.pos.x, struct.pos.y, 2)
                                } else {
                                    cm.set(struct.pos.x, struct.pos.y, 0xFF)
                                }
                            }
                        }

                        val roomCreeps = room.find(FIND_CREEPS)
                        if (roomCreeps.isNotEmpty()) {
                            for (creep in roomCreeps) {
                                cm.set(creep.pos.x, creep.pos.y, 2)
                            }
                        }
                    }

                    cm
                }
            })
        }
    }
}