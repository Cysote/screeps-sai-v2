package global

// These tags are used as the start of a flag name to trigger military operations. For example, a flag named "CLAIM" in
// a room will trigger a new Claim task to be created, and the nearest room will take that task and attempt to claim
// the room where the flag was placed. A flag named "CLAIM123" or "CLAIMdfgjhkls" will also trigger the task, but a flag
// named "abcdCLAIM" will not.

const val M_CLAIM_ROOM = "CLAIM"

val MILITARY_FLAG_DIRECTIVES = listOf(M_CLAIM_ROOM)