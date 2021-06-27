package creep.economy

import creep.actions.EconomyActions
import screeps.api.Creep

class IdleCreep(creep: Creep) : EconomyCreep, EconomyActions(creep) {
    override fun act() {
        // Do nothing
    }
}