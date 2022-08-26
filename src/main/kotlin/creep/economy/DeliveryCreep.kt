package creep.economy

import creep.actions.EconomyActions
import memory.dynamicDepositStructureId
import memory.reachedFullCapacity
import screeps.api.Creep

class DeliveryCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    init {
        updateCreepReachedFullCapacityMemory()
    }

    override fun act() {
        if (!creep.memory.reachedFullCapacity) {
            withdrawEnergyDynamic(throwException = false)
        }

        updateCreepReachedFullCapacityMemory()

        if (creep.memory.reachedFullCapacity) {
            if (!depositEnergyDynamic(throwException = false)) {
                creep.memory.dynamicDepositStructureId = ""
                dropEnergyNearConstructionSite(throwException = false)
            } else {
                depositEnergyDynamic(throwException = false)
            }
        }
    }
}