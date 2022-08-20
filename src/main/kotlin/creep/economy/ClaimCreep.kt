package creep.economy

import creep.actions.EconomyActions
import screeps.api.Creep

class ClaimCreep(private val creep: Creep): EconomyCreep, EconomyActions(creep) {
    override fun act() {

    }
}