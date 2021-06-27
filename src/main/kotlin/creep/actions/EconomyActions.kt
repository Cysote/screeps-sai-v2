package creep.actions

import annotations.ThrowsExceptions
import exception.ConstructionSiteNotFoundException
import exception.InvalidIdException
import screeps.api.*

@ThrowsExceptions
open class EconomyActions(private val creep: Creep) : MemoryActions(creep) {
    fun harvestTargetSource() {
        val source = getSourceFromTask()
        when (creep.harvest(source)) {
            ERR_NOT_IN_RANGE -> creep.moveTo(source)
        }
    }

    fun harvestAnySource() {
        val source = getSourceByTaskRoom()
        when (creep.harvest(source)) {
            ERR_NOT_IN_RANGE -> creep.moveTo(source)
        }
    }

    fun upgradeController() {
        val controller = getControllerFromTask()
        when (creep.upgradeController(controller)) {
            ERR_NOT_IN_RANGE -> creep.moveTo(controller)
        }
    }

    fun buildStructures(throwException: Boolean = true): Boolean {
        try {
            val constructionSite = getConstructionSiteByTaskRoom()
            when (creep.build(constructionSite)) {
                ERR_NOT_IN_RANGE -> creep.moveTo(constructionSite)
            }
        } catch (e: ConstructionSiteNotFoundException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun depositEnergy(throwException: Boolean = true): Boolean {
        try {
            val depositStructure = getDepositStructureFromTask()
            when (creep.transfer(depositStructure, RESOURCE_ENERGY)) {
                ERR_NOT_IN_RANGE -> creep.moveTo(depositStructure)
                ERR_FULL -> return false
            }
        } catch (e: InvalidIdException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun depositEnergyNearby(throwException: Boolean = true): Boolean {
        try {
            val depositStructure = getDepositStructureFromTask()
            creep.transfer(depositStructure, RESOURCE_ENERGY)
        } catch (e: InvalidIdException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun withdrawEnergy(throwException: Boolean = true): Boolean {
        try {
            val withdrawStructure = getWithdrawStructureFromTask()
            when (creep.withdraw(withdrawStructure, RESOURCE_ENERGY)) {
                ERR_NOT_IN_RANGE -> creep.moveTo(withdrawStructure)
                ERR_FULL -> return false
            }
        } catch (e: InvalidIdException) {
            if (throwException) throw e
            else return false
        }
        return true
    }
}