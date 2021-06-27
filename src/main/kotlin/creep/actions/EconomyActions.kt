package creep.actions

import annotations.ThrowsExceptions
import screeps.api.*

@ThrowsExceptions
open class EconomyActions(private val creep: Creep) : MemoryActions(creep) {
    fun harvestTargetSource() {
        val source = getSourceFromTask()
        when (creep.harvest(source)) {
            ERR_NOT_IN_RANGE -> creep.moveTo(source)
        }
    }

    fun depositEnergy() {
        val depositStructure = getDepositStructureFromTask()
        when (creep.transfer(depositStructure, RESOURCE_ENERGY)) {
            ERR_NOT_IN_RANGE -> creep.moveTo(depositStructure)
        }
    }

    fun depositEnergyNearby() {
        val depositStructure = getDepositStructureFromTask()
        creep.transfer(depositStructure, RESOURCE_ENERGY)
    }
}