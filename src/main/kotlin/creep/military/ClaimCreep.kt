package creep.military

import creep.actions.MilitaryActions
import screeps.api.Creep

class ClaimCreep(creep: Creep): MilitaryCreep, MilitaryActions(creep) {
    override fun act() {
        claimRoom()
    }
}