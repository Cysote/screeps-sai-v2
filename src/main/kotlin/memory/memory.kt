package memory

import info.ControllerInfo
import info.MineralInfo
import info.RoutingStructuresInfo
import info.SourceInfo
import screeps.api.*
import screeps.utils.memory.memory
import task.Task

var GlobalMemory.tasks: Array<Task> by memory { arrayOf<Task>() }
var GlobalMemory.lowPriorityRoomUpdateTicker: Int by memory { 0 }
var GlobalMemory.lowPriorityTaskUpdateTicker: Int by memory { 0 }

/* Creep.memory */
var CreepMemory.taskId: String by memory { "" }
var CreepMemory.reachedFullCapacity: Boolean by memory { false }
var CreepMemory.owningRoom: String by memory { "" }
var CreepMemory.dynamicSourceId: String by memory { "" }
var CreepMemory.dynamicWithdrawStructureId: String by memory { "" }
var CreepMemory.dynamicDepositStructureId: String by memory { "" }
var CreepMemory.dynamicPickupResourceId: String by memory { "" }

/* Rest of the persistent memory structures.
* These set an unused test variable to 0. This is done to illustrate the how to add variables to
* the memory. Change or remove it at your convenience.*/

/* Power creep is a late game hero unit that is spawned from a Power Spawn
   see https://docs.screeps.com/power.html for more details.
   This set sets up the memory for the PowerCreep.memory class.
 */
//var PowerCreepMemory.test : Int by memory { 0 }

/* flag.memory */
//var FlagMemory.test : Int by memory { 0 }

/* room.memory */
var RoomMemory.initialized : Boolean by memory { false }
var RoomMemory.level: Int by memory { 0 }
var RoomMemory.sourceInfos: Array<SourceInfo> by memory { arrayOf<SourceInfo>() }
var RoomMemory.mineralInfos: Array<MineralInfo> by memory { arrayOf<MineralInfo>() }
var RoomMemory.controllerInfo: ControllerInfo by memory { ControllerInfo() }
var RoomMemory.routingStructuresInfo: RoutingStructuresInfo by memory { RoutingStructuresInfo() }
var RoomMemory.totalControllerDistance: Int by memory { 0 }

/* spawn.memory */
var SpawnMemory.preparingToSpawn: Boolean by memory { false }