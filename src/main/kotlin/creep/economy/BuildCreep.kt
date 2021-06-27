package creep.economy

import annotations.ThrowsExceptions
import creep.actions.EconomyActions
import memory.reachedFullCapacity
import screeps.api.Creep

@ThrowsExceptions
class BuildCreep(private val creep: Creep) : EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepMemory()
    }

    override fun act() {
        if (!creep.memory.reachedFullCapacity) {
            if (!withdrawEnergy(throwException = false)) {
                harvestAnySource()
            }
        }

        updateCreepMemory()

        if (creep.memory.reachedFullCapacity) {
            buildStructures()
        }
    }
}