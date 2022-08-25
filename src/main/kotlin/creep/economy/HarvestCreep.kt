package creep.economy

import annotations.ThrowsExceptions
import creep.actions.EconomyActions
import memory.*
import screeps.api.*

@ThrowsExceptions
class HarvestCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepReachedFullCapacityMemory()
    }

    override fun act() {
        if (!creep.memory.reachedFullCapacity) {
            harvestTargetSource()
            if (!depositEnergyNearby(throwException = false)) {
                flagTaskAsNeedingMaintenance()
            }
        }

        updateCreepReachedFullCapacityMemory()

        if (creep.memory.reachedFullCapacity) {
            if (!depositEnergyEconomic(throwException = false)) {
                if (!buildStructures(throwException = false)) {
                    upgradeController()
                }
            }
        }
    }
}