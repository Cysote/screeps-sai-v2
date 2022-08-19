package info

import screeps.api.RoomPosition

data class SourceInfo(
        val sourceId: String = "",
        var sourceContainerId: String = "",
        var sourceLinkId: String = "",
        var pathToSource: Array<RoomPosition> = arrayOf()
)