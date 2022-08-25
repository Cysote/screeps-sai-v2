package creep.actions

import screeps.api.*

open class CustomActions(private val creep: Creep) {

    fun moveAwayFrom(location: RoomPosition, distance: Int) {
        if (creep.pos.inRangeTo(location, distance - 1)) {
            val foundPath = PathFinder.search(creep.pos, location, options {
                flee = true
            })

            if (!foundPath.incomplete) {
                creep.moveByPath(foundPath.path)
            }
        }
    }
}