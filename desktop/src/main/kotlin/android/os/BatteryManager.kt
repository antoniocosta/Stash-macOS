package android.os

/**
 * Desktop shim for android.os.BatteryManager constants. The sticky ACTION_BATTERY_CHANGED
 * Intent carrying these extras is synthesized from `pmset -g batt` by Context.registerReceiver.
 */
class BatteryManager private constructor() {
    companion object {
        const val EXTRA_STATUS = "status"
        const val EXTRA_PRESENT = "present"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_SCALE = "scale"
        const val EXTRA_PLUGGED = "plugged"

        const val BATTERY_STATUS_UNKNOWN = 1
        const val BATTERY_STATUS_CHARGING = 2
        const val BATTERY_STATUS_DISCHARGING = 3
        const val BATTERY_STATUS_NOT_CHARGING = 4
        const val BATTERY_STATUS_FULL = 5

        const val BATTERY_PLUGGED_AC = 1
        const val BATTERY_PLUGGED_USB = 2
        const val BATTERY_PLUGGED_WIRELESS = 4

        internal val INSTANCE = BatteryManager()
    }
}
