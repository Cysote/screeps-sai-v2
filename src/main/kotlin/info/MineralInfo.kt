package info

import screeps.api.MineralConstant
import screeps.api.RESOURCE_HYDROGEN

data class MineralInfo(
        val mineralId: String = "",
        val mineralType: MineralConstant = RESOURCE_HYDROGEN,
        var mineralContainerId: String = ""
)