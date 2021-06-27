package info

import screeps.api.RoomPosition

data class SourceInfo(
        val sourceId: String = "",
        var sourceContainerId: String = "",
        var sourceLinkId: String = "",
        var serializedPathToSource: Array<RoomPosition> = arrayOf(),
        var hasRoads: Boolean = false
)