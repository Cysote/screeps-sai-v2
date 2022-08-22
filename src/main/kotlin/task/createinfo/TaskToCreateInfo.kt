package task.createinfo

data class TaskToCreateInfo(
        val owningRoom: String = "",
        val taskType: String = "",
        val targetRoom: String = ""
)