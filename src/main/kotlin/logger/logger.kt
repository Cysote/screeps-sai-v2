package logger

import debug
import screeps.api.Game
import shardName

// <a href="#!/room/W41N1">W41N1</a>

fun logMessage(message: String) {
    console.log(message)
}

fun logMessage(message: String, roomName: String) {
    val messageWithLink = message.replace(roomName, """<a href="#!/room/$shardName/$roomName">$roomName</a>""", false)
    console.log(messageWithLink)
}

fun logPriorityMessage(message: String) {
    console.log("<font color=green> $message </font>")
}

fun logPriorityMessage(message: String, roomName: String) {
    val messageWithLink = message.replace(roomName, """<a href="#!/room/$shardName/$roomName">$roomName</a>""", false)
    console.log("<font color=green> $messageWithLink </font>")
}

fun logMilitaryMessage(message: String) {
    console.log("<font color=deepskyblue> $message </font>")
}

fun logMilitaryMessage(message: String, roomName: String) {
    val messageWithLink = message.replace(roomName, """<a href="#!/room/$shardName/$roomName">$roomName</a>""", false)
    console.log("<font color=deepskyblue> $messageWithLink </font>")
}

fun logError(message: String) {
    console.log("<font color=red> $message </font>")
}

fun logError(message: String, roomName: String) {
    val messageWithLink = message.replace(roomName, """<a href="#!/room/$shardName/$roomName">$roomName</a>""", false)
    console.log("<font color=red> $messageWithLink </font>")
}

fun logCatastrophicError(message: String) {
    Game.notify(message)
    console.log("<font color=red> $message </font>")
}

fun logCatastrophicError(message: String, roomName: String) {
    val messageWithLink = message.replace(roomName, """<a href="#!/room/$shardName/$roomName">$roomName</a>""", false)
    Game.notify(messageWithLink)
    console.log("<font color=red> $messageWithLink </font>")
}

fun logDebugMessage(message: String) {
    if (debug) console.log("<font color=yellow>Debug: $message </font>")
}