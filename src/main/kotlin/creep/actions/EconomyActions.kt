package creep.actions

import annotations.ThrowsExceptions
import exception.InvalidIdException
import logger.logMessage
import memory.dynamicDepositStructureId
import memory.dynamicPickupResourceId
import memory.dynamicSourceId
import memory.dynamicWithdrawStructureId
import screeps.api.*

@ThrowsExceptions
open class EconomyActions(private val creep: Creep) : MemoryActions(creep) {

    /**
     * Harvests a known source
     */
    fun harvestTargetSource() {
        val source = getSourceFromTask()
        when (creep.harvest(source)) {
            ERR_NOT_IN_RANGE -> creep.moveTo(source)
        }
    }

    /**
     * Dynamically finds a source and harvests it
     */
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

    /**
     * Attempts to upgrade the room controller
     */
    fun upgradeController(throwException: Boolean = true): Boolean {
        try {
            val controller = getControllerFromTask()
            when (creep.upgradeController(controller)) {
                ERR_NOT_IN_RANGE -> creep.moveTo(controller)
            }
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }

    /**
     * Constructs buildings in a room based on the order the construction sites was instantiated in
     */
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

    /**
     * Deposits energy based on known structures
     */
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

    /**
     * Dynamically finds only economic structures to deposit energy into
     */
    fun depositEnergyEconomic(throwException: Boolean = true): Boolean {
        try {
            if (creep.memory.dynamicDepositStructureId.isBlank()) {
                creep.memory.dynamicDepositStructureId = getEconomicDepositStructureByTaskRoom().id
            }
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return dynamicEnergyDepositing(throwException)
    }

    /**
     * Dynamically finds structures to deposit energy into
     */
    fun depositEnergyDynamic(throwException: Boolean = true): Boolean {
        try {
            if (creep.memory.dynamicDepositStructureId.isBlank()) {
                creep.memory.dynamicDepositStructureId = getDepositStructureByTaskRoom().id
            }
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return dynamicEnergyDepositing(throwException)
    }

    /**
     * Common depositing logic used by depositEnergyEconomic and depositEnergyDynamic
     */
    private fun dynamicEnergyDepositing(throwException: Boolean): Boolean {
        try {
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

    fun dropEnergyNearConstructionSite(throwException: Boolean = true): Boolean {
        try {
            val constructionSite = getConstructionSiteByTaskRoom()
            if (creep.pos.isNearTo(constructionSite)) creep.drop(RESOURCE_ENERGY)
            else creep.moveTo(constructionSite)
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

    fun pickupDroppedEnergyNearConstructionSite(throwException: Boolean = true): Boolean {
        try {
            logMessage("Creep: ${creep.name}")
            var pickupResource: Resource? = null
            if (creep.memory.dynamicPickupResourceId.isBlank()) {
                val constructionSite = getConstructionSiteByTaskRoom()

                val top = constructionSite.pos.y - 1
                val bottom = constructionSite.pos.y + 1
                val left = constructionSite.pos.x - 1
                val right = constructionSite.pos.x + 1
                val result = creep.room.lookAtAreaAsArray(top, left, bottom, right)

                logMessage("result:")
                val energyList = result.filter { it.type == LOOK_ENERGY }
                energyList.forEach {
                    logMessage("Type: " + it.type.toString())
                }

                val energy = energyList[0]


                pickupResource = result.filter { it.type == LOOK_ENERGY }[0].resource
                if (pickupResource != null) creep.memory.dynamicPickupResourceId = pickupResource.id
            }

            if (pickupResource == null) {
                logMessage("Null Resource")
                pickupResource = Game.getObjectById<Resource>(creep.memory.dynamicPickupResourceId)
                        ?: throw InvalidIdException("dynamicPickupResourceId mapped to no real structure. Creep: ${creep.name}")
            }
            logMessage("ID: ${creep.memory.dynamicPickupResourceId}")
            when (creep.pickup(pickupResource)) {
                ERR_NOT_IN_RANGE -> creep.moveTo(pickupResource)
                ERR_FULL -> return false
                OK -> creep.memory.dynamicPickupResourceId = ""
            }
        } catch (e: RuntimeException) {
            if (throwException) throw e
            else return false
        }
        return true
    }
}