import manager.ColonyManager

/**
 * Entry point
 * is called by screeps
 *
 * must not be removed by DCE
 */
@Suppress("unused")
fun loop() {
    val cm = ColonyManager()
    cm.run()
}

const val debug = false
const val shardName = "shard3"