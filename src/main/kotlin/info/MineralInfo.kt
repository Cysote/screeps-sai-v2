package info

import screeps.api.MineralConstant

data class MineralInfo(
        val mineralId: String = "",
        val mineralType: MineralConstant,
        var mineralContainerId: String = "",
        var mineralLinkId: String = ""
)