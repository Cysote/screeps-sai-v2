package task

data class Task(
        var id: String = "",                            // The unique ID of this task
        var role: String = TaskRole.NONE.name,          // Whether this task is for the Economy or Military
        var type: String = TaskType.NONE.name,          // Describes what kind of creeps should work this task
        var isActive: Boolean = false,                  // Whether this task should be worked on right now
        var owningRoom: String = "",                    // The room that owns this task
        var targetId: String = "",                      // The ID of the target of this task
        var depositStructureId: String = "",            // The ID of the structure to deposit resources into
        var withdrawStructureId: String = "",           // The ID of the structure to withdraw resources from
        var bHasRoads: Boolean = false,                 // Whether the task has roads (to allow creeps to be spawned with less MOVE)
        var assignedCreeps: Array<String> = arrayOf(),  // The list of creeps currently working this task
        var desiredCreeps: Int = 0,                     // The maximum amount of creeps to work this task. 0 for no limit.
        var desiredWork: Int = 0,                       // The total work needed to fill this task
        var desiredCarry: Int = 0,                      // The total carry needed to fill this task
): ColonyTask