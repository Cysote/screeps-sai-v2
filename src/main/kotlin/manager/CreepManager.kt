package manager

import annotations.ThrowsExceptions
import creep.economy.HarvestCreep
import creep.economy.IdleCreep
import exception.TaskTypeNotSupportedException
import logger.logDebugMessage
import logger.logError
import memory.owningRoom
import memory.taskId
import memory.tasks
import screeps.api.*
import task.Task
import task.TaskType
import kotlin.math.max

/**
 * Manages all direct actions to creeps across the entire colony.
 */
class CreepManager {
    private val idleCreeps: List<Creep>

    init {
        idleCreeps = getIdleCreeps()
    }

    fun activateAliveCreeps() {
        Game.creeps.values.forEach { creep ->
            val creepTask = Memory.tasks.singleOrNull { memTask -> memTask.id == creep.memory.taskId }
            if (creepTask == null) {
                logError("Creep: ${creep.name} has an id with no task")
                IdleCreep(creep).act()
            } else {
                try {
                    when(creepTask.type) {
                        TaskType.HARVESTSOURCE.name -> HarvestCreep(creep)
                        else -> IdleCreep(creep)
                    }.act()
                }
                catch (e: RuntimeException) {
                    logError("Creep failed to act. Reason: ${e.message}")
                }
            }
        }
    }

    fun getIdleCreepForTask(task: Task): Creep? {
        idleCreeps.forEach { creep ->
            if (creep.memory.owningRoom == task.owningRoom) {
                when (task.type) {
                    TaskType.HARVESTSOURCE.name,
                        TaskType.HARVESTREMOTE.name,
                        TaskType.HARVESTMINERAL.name -> {
                        when (task.desiredCreeps) {
                            1 -> {
                                if (getBodyNum(creep, WORK) == task.desiredWork && getBodyNum(creep, CARRY) > 0)
                                    return creep
                            }
                            else -> if (getBodyNum(creep, WORK) > 0 && getBodyNum(creep, CARRY) > 0)
                                return creep
                        }
                    }
                    else -> return creep
                }
            }
        }
        return null
    }

    private fun getBodyNum(creep: Creep, bodyPartConstant: BodyPartConstant): Int {
        return creep.body.filter { it.type == bodyPartConstant }.size
    }

    private fun getIdleCreeps(): List<Creep> {
        val idleCreeps = mutableListOf<Creep>()
        Game.creeps.values.forEach { creep ->
            if (creep.memory.taskId.isBlank()) {
                idleCreeps.add(creep)
            }
        }

        return idleCreeps
    }

    @ThrowsExceptions
    fun getCreepBodyForTask(task: Task, maxEnergy: Int): List<BodyPartConstant> {
        val workNeeded: Int
        val workRatio: Int

        val carryNeeded: Int
        val carryRatio: Int

        // These can stay initialized because only CLAIM and RESERVE creeps care
        var claimNeeded = 0
        var claimRatio = 0

        var moveNeeded: Int
        val moveRatio: Int

        when (task.type) {
            TaskType.HARVESTSOURCE.name -> {
                // Do not subtract the work we have already provisioned, because we want a single creep to own a source eventually
                workNeeded = task.desiredWork
                workRatio = 1

                carryNeeded = 1
                carryRatio = 0

                moveNeeded = workNeeded + carryNeeded
                moveRatio = 1
            }
            else -> {
                throw TaskTypeNotSupportedException("Attempted to create a body for task ${task.type} but no code path exists.")
            }
        }

        return generateBodyByRatio(
                maxEnergy = maxEnergy,
                maxWork = workNeeded, workRatio = workRatio,
                maxCarry = carryNeeded, carryRatio = carryRatio,
                maxClaim = claimNeeded, claimRatio = claimRatio,
                maxMove = moveNeeded, moveRatio = moveRatio)
    }

    /**
     * With the given maxEnergy as the cap, attempt to create a creep body that uses the given ratios of work, carry, and move
     * up to the maximum specified for each body part. If no body could be generated at all, return a W1:C1:M2 body instead.
     * If the given max was 1 and the ratio was 0, then add one body part and a corresponding move part.
     *
     * Example creep body generation.
     * Assuming that these parameters are given to the generator:
     * maxWork = 6, workRatio = 1
     * maxCarry = 1, carryRatio = 0
     * maxMove = 7, moveRatio = 1
     *
     * With maxEnergy = 550 (RCL 2 + extensions) the generated creep is [WORK, WORK, WORK, CARRY, MOVE, MOVE, MOVE, MOVE].
     * A total of 550 energy spent. This is because a 1:1 WORK:MOVE ratio adds 1 work and 1 move together, and the 1
     * carry and 0 carry ratio together forces a single carry and move to be added without further attempting more
     * calculations.
     *
     * With maxEnergy = 800 (RCL 3 + extensions) the generated creep is [WORK, WORK, WORK, WORK, CARRY, MOVE, MOVE,
     * MOVE, MOVE, MOVE], a total of 700 energy spent. It doesn't reach 800 energy spent because the ratios are
     * 1:1 WORK:MOVE, meaning that adding one more WORK (100 energy) forces another MOVE to be added (50 energy) and the
     * resulting body would be over the given max of 800.
     *
     * Body Parts are added to the creep in the following order:
     * TOUGH -> WORK -> CARRY -> CLAIM -> ATTACK -> RANGED_ATTACK -> HEAL -> MOVE
     */
    fun generateBodyByRatio(
            maxEnergy: Int,
            maxWork: Int = 0, workRatio: Int = 0,
            maxCarry: Int = 0, carryRatio: Int = 0,
            maxClaim: Int = 0, claimRatio: Int = 0,
            maxAttack: Int = 0, attackRatio: Int = 0,
            maxRangedAttack: Int = 0, rangedAttackRatio: Int = 0,
            maxTough: Int = 0, toughRatio: Int = 0,
            maxHeal: Int = 0, healRatio: Int = 0,
            maxMove: Int = 0, moveRatio: Int = 0
    ): List<BodyPartConstant> {
        logDebugMessage("Generate Body By Ratio Parameters - maxEnergy: $maxEnergy,\n" +
                "            maxWork: $maxWork, workRatio: $workRatio,\n" +
                "            maxCarry: $maxCarry, carryRatio: $carryRatio,\n" +
                "            maxClaim: $maxClaim, claimRatio: $claimRatio,\n" +
                "            maxAttack: $maxAttack, attackRatio: $attackRatio,\n" +
                "            maxRangedAttack: $maxRangedAttack, rangedAttackRatio: $rangedAttackRatio,\n" +
                "            maxTough: $maxTough, toughRatio: $toughRatio,\n" +
                "            maxHeal: $maxHeal, healRatio: $healRatio,\n" +
                "            maxMove: $maxMove, moveRatio: $moveRatio")

        // Figure out the max parts we can have for the max energy we can use
        val maxParts = 50

        var workParts = 0
        var carryParts = 0
        var claimParts = 0
        var attackParts = 0
        var rangedAttackParts = 0
        var toughParts = 0
        var healParts = 0
        var moveParts = 0
        var energyUsed = 0

        // If a ratio was 0, and max was 1, add 1
        if (maxWork == 1 && workRatio == 0) {
            workParts = 1
            energyUsed += BODYPART_COST[WORK]!!
            moveParts += 1
            energyUsed += BODYPART_COST[MOVE]!!
        }
        if (maxCarry == 1 && carryRatio == 0) {
            carryParts = 1
            energyUsed += BODYPART_COST[CARRY]!!
            moveParts += 1
            energyUsed += BODYPART_COST[MOVE]!!
        }
        if (maxClaim == 1 && claimRatio == 0) {
            claimParts = 1
            energyUsed += BODYPART_COST[CLAIM]!!
            moveParts += 1
            energyUsed += BODYPART_COST[MOVE]!!
        }
        if (maxAttack == 1 && attackRatio == 0) {
            attackParts = 1
            energyUsed += BODYPART_COST[ATTACK]!!
            moveParts += 1
            energyUsed += BODYPART_COST[MOVE]!!
        }
        if (maxRangedAttack == 1 && rangedAttackRatio == 0) {
            rangedAttackParts = 1
            energyUsed += BODYPART_COST[RANGED_ATTACK]!!
            moveParts += 1
            energyUsed += BODYPART_COST[MOVE]!!
        }
        if (maxTough == 1 && toughRatio == 0) {
            toughParts = 1
            energyUsed += BODYPART_COST[TOUGH]!!
            moveParts += 1
            energyUsed += BODYPART_COST[MOVE]!!
        }
        if (maxHeal == 1 && healRatio == 0) {
            healParts = 1
            energyUsed += BODYPART_COST[HEAL]!!
            moveParts += 1
            energyUsed += BODYPART_COST[MOVE]!!
        }
        if (maxMove == 1 && moveRatio == 0) {
            moveParts = 1
            energyUsed += BODYPART_COST[MOVE]!!
        }
        logDebugMessage("Initial parts - Work: $workParts, Carry: $carryParts, Claim: $claimParts, " +
                "Attack: $attackParts, RangedAttack: $rangedAttackParts, Tough: $toughParts " +
                "Heal: $healParts, Move: $moveParts - Initial energy: $energyUsed")

        var nextRunEnergyUsed = 0

        // Stage the results of the potential next run as the while loop's test
        nextRunEnergyUsed = energyUsed
        if (workParts + workRatio <= maxWork) nextRunEnergyUsed += (workRatio * BODYPART_COST[WORK]!!)
        if (carryParts + carryRatio <= maxCarry) nextRunEnergyUsed += (carryRatio * BODYPART_COST[CARRY]!!)
        if (claimParts + claimRatio <= maxClaim) nextRunEnergyUsed += (claimRatio * BODYPART_COST[CLAIM]!!)
        if (attackParts + attackRatio <= maxAttack) nextRunEnergyUsed += (attackRatio * BODYPART_COST[ATTACK]!!)
        if (rangedAttackParts + rangedAttackRatio <= maxRangedAttack) nextRunEnergyUsed += (rangedAttackRatio * BODYPART_COST[RANGED_ATTACK]!!)
        if (toughParts + toughRatio <= maxTough) nextRunEnergyUsed += (toughRatio * BODYPART_COST[TOUGH]!!)
        if (healParts + healRatio <= maxHeal) nextRunEnergyUsed += (healRatio * BODYPART_COST[HEAL]!!)
        if (moveParts + moveRatio <= maxMove) nextRunEnergyUsed += (moveRatio * BODYPART_COST[MOVE]!!)
        while(nextRunEnergyUsed <= maxEnergy
                && (workParts+workRatio + carryParts+carryRatio + claimParts+claimRatio + attackParts+attackRatio + rangedAttackParts+rangedAttackRatio + toughParts+toughRatio + healParts+healRatio + moveParts+moveRatio) <= maxParts
                && (((workParts + workRatio <= maxWork && workRatio != 0))
                        || ((carryParts + carryRatio <= maxCarry && carryRatio != 0))
                        || ((claimParts + claimRatio <= maxClaim && claimRatio != 0))
                        || ((attackParts + attackRatio <= maxAttack && attackRatio != 0))
                        || ((rangedAttackParts + rangedAttackRatio <= maxRangedAttack && rangedAttackRatio != 0))
                        || ((toughParts + toughRatio <= maxTough && toughRatio != 0))
                        || ((healParts + healRatio <= maxHeal && healRatio != 0))
                        || ((moveParts + moveRatio <= maxMove && moveRatio != 0)))) {

            if (workParts + workRatio <= maxWork) {
                workParts += workRatio
                energyUsed += (workRatio * BODYPART_COST[WORK]!!)
            }

            if (carryParts + carryRatio <= maxCarry) {
                carryParts += carryRatio
                energyUsed += (carryRatio * BODYPART_COST[CARRY]!!)
            }

            if (claimParts + claimRatio <= maxClaim) {
                claimParts += claimRatio
                energyUsed += (claimRatio * BODYPART_COST[CLAIM]!!)
            }

            if (attackParts + attackRatio <= maxAttack) {
                attackParts += attackRatio
                energyUsed += (attackRatio * BODYPART_COST[ATTACK]!!)
            }

            if (rangedAttackParts + rangedAttackRatio <= maxRangedAttack) {
                rangedAttackParts += rangedAttackRatio
                energyUsed += (rangedAttackRatio * BODYPART_COST[RANGED_ATTACK]!!)
            }

            if (toughParts + toughRatio <= maxTough) {
                toughParts += toughRatio
                energyUsed += (toughRatio * BODYPART_COST[TOUGH]!!)
            }

            if (healParts + healRatio <= maxHeal) {
                healParts += healRatio
                energyUsed += (healRatio * BODYPART_COST[HEAL]!!)
            }

            if (moveParts + moveRatio <= maxMove) {
                moveParts += moveRatio
                energyUsed += (moveRatio * BODYPART_COST[MOVE]!!)
            }

            // Stage the results of the potential next run as the while loop's test
            nextRunEnergyUsed = energyUsed
            if (workParts + workRatio <= maxWork) nextRunEnergyUsed += (workRatio * BODYPART_COST[WORK]!!)
            if (carryParts + carryRatio <= maxCarry) nextRunEnergyUsed += (carryRatio * BODYPART_COST[CARRY]!!)
            if (claimParts + claimRatio <= maxClaim) nextRunEnergyUsed += (claimRatio * BODYPART_COST[CLAIM]!!)
            if (attackParts + attackRatio <= maxAttack) nextRunEnergyUsed += (attackRatio * BODYPART_COST[ATTACK]!!)
            if (rangedAttackParts + rangedAttackRatio <= maxRangedAttack) nextRunEnergyUsed += (rangedAttackRatio * BODYPART_COST[RANGED_ATTACK]!!)
            if (toughParts + toughRatio <= maxTough) nextRunEnergyUsed += (toughRatio * BODYPART_COST[TOUGH]!!)
            if (healParts + healRatio <= maxHeal) nextRunEnergyUsed += (healRatio * BODYPART_COST[HEAL]!!)
            if (moveParts + moveRatio <= maxMove) nextRunEnergyUsed += (moveRatio * BODYPART_COST[MOVE]!!)
        }

        logDebugMessage("Generated parts - EnergyUsed: $energyUsed, Work: $workParts, Carry: $carryParts, Claim: $claimParts, " +
                "Attack: $attackParts, RangedAttack: $rangedAttackParts, Tough: $toughParts, Heal: $healParts, Move: $moveParts")

        // This shouldn't ever happen!
        if (workParts < 0 || carryParts < 0 || claimParts < 0 || attackParts < 0 || rangedAttackParts < 0 || toughParts < 0 || healParts < 0 || moveParts < 0) {
            logError("Part of a generated body for a task was less than zero! Work: $workParts, Carry: $carryParts, Claim: $claimParts, " +
                    "Attack: $attackParts, RangedAttack $rangedAttackParts, Tough: $toughParts, Heal: $healParts, Move: $moveParts")
            return listOf(WORK, CARRY, MOVE, MOVE)
        }

        // Create body using above values
        val body = arrayListOf<BodyPartConstant>()

        // Tough first, so that it's the first part to get attacked by enemies
        while (toughParts > 0) {
            body.add(TOUGH)
            toughParts--
        }
        while (workParts > 0) {
            body.add(WORK)
            workParts--
        }
        while (carryParts > 0) {
            body.add(CARRY)
            carryParts--
        }
        while (claimParts > 0) {
            body.add(CLAIM)
            claimParts--
        }
        while (attackParts > 0) {
            body.add(ATTACK)
            attackParts--
        }
        while (rangedAttackParts > 0) {
            body.add(RANGED_ATTACK)
            rangedAttackParts--
        }
        while (healParts > 0) {
            body.add(HEAL)
            healParts--
        }
        while (moveParts > 0) {
            body.add(MOVE)
            moveParts--
        }

        return body
    }
}