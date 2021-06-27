package task

enum class TaskType(val priority: Int) {

    // These tasks are performed inside of the main room
    HARVESTSOURCE(0),
    DELIVERY(1),
    ROUTER(2),
    BUILD(3),
    UPGRADE(4),
    REPAIR(5),
    HARVESTMINERAL(6),

    // These tasks are external to the main room
    SCOUT(7),
    HARVESTREMOTE(8),
    DELIVERYREMOTE(8),
    RESERVE(8),
    CLAIM(9),

    // If the creep could possibly perform a new job
    IDLE(99),

    // This should never happen
    NONE(100)
}