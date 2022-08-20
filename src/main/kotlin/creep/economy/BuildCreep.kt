package creep.economy

import annotations.ThrowsExceptions
import creep.actions.EconomyActions
import logger.logMessage
import memory.reachedFullCapacity
import screeps.api.Creep

@ThrowsExceptions
class BuildCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepMemory()
    }

    override fun act() {
        if (!creep.memory.reachedFullCapacity) {
            if (!pickupDroppedEnergyNearConstructionSite(throwException = false)) {
                if (!withdrawEnergy(throwException = false)) {
                    harvestSourceDynamic()
                }
            }
        }

        updateCreepMemory()

        if (creep.memory.reachedFullCapacity) {
            if (!buildStructures(throwException = false)) {
                upgradeController()
            }
        }
    }
}