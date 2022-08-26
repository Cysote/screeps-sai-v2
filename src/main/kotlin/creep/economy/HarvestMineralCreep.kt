package creep.economy

import creep.actions.EconomyActions
import memory.reachedFullCapacity
import screeps.api.Creep

class HarvestMineralCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepReachedFullCapacityMemory()
    }

    override fun act() {
        if (!creep.memory.reachedFullCapacity) {
            harvestTargetMineral()
            if (!depositResourceNearby(throwException = false)) {
                flagTaskAsNeedingMaintenance()
            }
        }

        updateCreepReachedFullCapacityMemory()

        if (creep.memory.reachedFullCapacity) {
            depositResourceInStorage()
        }
    }
}