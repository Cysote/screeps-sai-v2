package creep.actions

import annotations.ThrowsExceptions
import exception.ConstructionSiteNotFoundException
import exception.InvalidIdException
import logger.logMessage
import memory.dynamicDepositStructureId
import memory.dynamicSourceId
import memory.dynamicWithdrawStructureId
import screeps.api.*
import screeps.api.structures.Structure

@ThrowsExceptions
open class EconomyActions(private val creep: Creep) : MemoryActions(creep) {
    fun harvestTargetSource() {
        val source = getSourceFromTask()
        when (creep.harvest(source)) {
            ERR_NOT_IN_RANGE -> creep.moveTo(source)
        }
    }

    fun harvestSourceDynamic() {
        if (creep.memory.dynamicSourceId.isBlank()) {
            creep.memory.dynamicSourceId = getSourceByTaskRoom().id
        }
        val source = Game.getObjectById<Source>(creep.memory.dynamicSourceId)
                ?: throw InvalidIdException("dynamicDepositStructureId mapped to no real structure. Creep: ${creep.name}")
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
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun depositEnergy(throwException: Boolean = true): Boolean {
        try {
            val depositStructure = getDepositStructureFromTask()
            if (depositStructure.store.getFreeCapacity() == 0) return false
            when (creep.transfer(depositStructure, RESOURCE_ENERGY)) {
                ERR_NOT_IN_RANGE -> creep.moveTo(depositStructure)
                ERR_FULL -> return false
            }
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun depositEnergyDynamic(throwException: Boolean = true): Boolean {
        try {
            if (creep.memory.dynamicDepositStructureId.isBlank()) {
                creep.memory.dynamicDepositStructureId = getDepositStructureByTaskRoom().id
            }
            val depositStructure = Game.getObjectById<StoreOwner>(creep.memory.dynamicDepositStructureId)
                    ?: throw InvalidIdException("dynamicDepositStructureId mapped to no real structure. Creep: ${creep.name}")
            if (depositStructure.store.getFreeCapacity(RESOURCE_ENERGY) == 0) {
                creep.memory.dynamicDepositStructureId = ""
            } else {
                when (creep.transfer(depositStructure, RESOURCE_ENERGY)) {
                    ERR_NOT_IN_RANGE -> creep.moveTo(depositStructure)
                    ERR_FULL -> return false
                    OK -> creep.memory.dynamicDepositStructureId = ""
                }
            }
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun depositEnergyNearby(throwException: Boolean = true): Boolean {
        try {
            val depositStructure = getDepositStructureFromTask()
            creep.transfer(depositStructure, RESOURCE_ENERGY)
        } catch (e: RuntimeException) {
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
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun withdrawEnergyDynamic(throwException: Boolean = true): Boolean {
        try {
            if (creep.memory.dynamicWithdrawStructureId.isBlank()) {
                creep.memory.dynamicWithdrawStructureId = getWithdrawStructureByTaskRoom().id
            }
            val withdrawStructure = Game.getObjectById<StoreOwner>(creep.memory.dynamicWithdrawStructureId)
                    ?: throw InvalidIdException("dynamicWithdrawStructureId mapped to no real structure. Creep: ${creep.name}")
            if (withdrawStructure.store.getUsedCapacity(RESOURCE_ENERGY) < withdrawStructure.store.getCapacity(RESOURCE_ENERGY)!!/2) {
                creep.memory.dynamicWithdrawStructureId = ""
            } else {
                when (creep.withdraw(withdrawStructure, RESOURCE_ENERGY)) {
                    ERR_NOT_IN_RANGE -> creep.moveTo(withdrawStructure)
                    ERR_FULL -> return false
                    OK -> creep.memory.dynamicWithdrawStructureId = ""
                }
            }
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun withdrawEnergyNearby(throwException: Boolean = true): Boolean {
        try {
            val withdrawStructure = getWithdrawStructureFromTask()
            creep.withdraw(withdrawStructure, RESOURCE_ENERGY)
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    fun withdrawEnergyNearController(throwException: Boolean = true): Boolean {
        try {
            val withdrawStructure = getWithdrawStructureByRoomController()
            creep.withdraw(withdrawStructure, RESOURCE_ENERGY)
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }
}