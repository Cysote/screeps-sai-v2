package creep.economy

import annotations.ThrowsExceptions
import creep.actions.EconomyActions
import logger.logMessage
import memory.*
import screeps.api.*

@ThrowsExceptions
class HarvestCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepMemory()
    }

    override fun act() {
        if (!creep.memory.reachedFullCapacity) {
            harvestTargetSource()
            depositEnergyNearby(throwException = false)
        }

        updateCreepMemory()

        if (creep.memory.reachedFullCapacity) {
            if (!depositEnergyEconomic(throwException = false)) {
                if (!buildStructures(throwException = false)) {
                    upgradeController()
                }
            }
        }
    }
}