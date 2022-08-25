package creep.economy

import creep.actions.EconomyActions
import memory.reachedFullCapacity
import screeps.api.Creep

class RepairCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepReachedFullCapacityMemory()
    }

    override fun act() {
        if (!creep.memory.reachedFullCapacity) {
            withdrawEnergyDynamic(throwException = false)
        }

        updateCreepReachedFullCapacityMemory()

        if (creep.memory.reachedFullCapacity) {
            if (!repairStructures(throwException = false)) {
                repairStructuresDynamic()
            }
        }
    }
}