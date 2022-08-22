package global

// These tags are used as the start of a flag name to create a new construction site for the relevant structure
// when the room where this flag is in levels up. For example, if a flag's name starts with "EXT", then when the
// room levels up, that flag will be deleted and a new construction site for an extension will be created. Flags with
// names such as "EXT23490" or "EXTdkjla" will also create a new extension construction site, but a name such as
// "abcdEXT" will not. The flag name must start with this tag

const val ROAD_TAG = "ROAD"
const val RAMPART_TAG = "RAMPART"
const val EXTENSION_TAG = "EXT"
const val CONTAINER_TAG = "CONT"
const val TOWER_TAG = "TOWER"
const val STORAGE_TAG = "STORAGE"
const val LINK_TAG = "LINK"
const val EXTRACTOR_TAG = "EXTRACTOR"
const val LAB_TAG = "LAB"
const val TERMINAL_TAG = "TERMINAL"
const val FACTORY_TAG = "FACTORY"
const val SPAWN_TAG = "SPAWN"
const val OBSERVER_TAG = "OBSERVER"
const val POWER_SPAWN_TAG = "POWERSPAWN"
const val NUKER_TAG = "NUKER"