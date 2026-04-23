package site.meowcat.pkn.`object`

import site.meowcat.pkn.model.DeviceStats

object TrafficStats {
    val stats = mutableMapOf<String, DeviceStats>()

    @Synchronized
    fun record(src: String, dst: String, size: Int) {
        val s = stats.getOrPut(src) { DeviceStats(src) }
        s.sentBytes += size

    }
}