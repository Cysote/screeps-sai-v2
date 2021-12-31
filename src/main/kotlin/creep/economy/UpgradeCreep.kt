package creep.economy

import creep.actions.EconomyActions
import memory.reachedFullCapacity
import screeps.api.Creep

class UpgradeCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepMemory()
    }

    override fun act() {
        if (creep.memory.reachedFullCapacity) {
            upgradeController()
            withdrawEnergyNearby(throwException = false)
        }

        updateCreepMemory()

        if (!creep.memory.reachedFullCapacity) {
            withdrawEnergy()
        }
    }
}