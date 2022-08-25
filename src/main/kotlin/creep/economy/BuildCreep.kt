package creep.economy

import annotations.ThrowsExceptions
import creep.actions.EconomyActions
import memory.reachedFullCapacity
import screeps.api.Creep

@ThrowsExceptions
class BuildCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepReachedFullCapacityMemory()
    }

    override fun act() {
        if (!creep.memory.reachedFullCapacity) {
            if (!pickupDroppedEnergyNearConstructionSite(throwException = false)) {
                if (!withdrawEnergy(throwException = false)) {
                    harvestSourceDynamic()
                }
            }
        }

        updateCreepReachedFullCapacityMemory()

        if (creep.memory.reachedFullCapacity) {
            if (!buildStructures(throwException = false)) {
                upgradeController()
            }
        }
    }
}