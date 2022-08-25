package creep.economy

import creep.actions.EconomyActions
import memory.reachedFullCapacity
import screeps.api.Creep

class UpgradeCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepReachedFullCapacityMemory()
    }

    override fun act() {
        if (creep.memory.reachedFullCapacity) {
            upgradeController()
            withdrawEnergyNearby(throwException = false)
        }

        updateCreepReachedFullCapacityMemory()

        if (!creep.memory.reachedFullCapacity) {
            withdrawEnergy()
        }
    }
}