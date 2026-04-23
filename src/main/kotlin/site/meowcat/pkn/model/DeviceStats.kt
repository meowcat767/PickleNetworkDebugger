package site.meowcat.pkn.model

data class DeviceStats(
    val ip: String,
    var sentBytes: Long = 0,
    var recvBytes: Long = 0,
    var sentPackets: Long = 0,
    var recvPackets: Long = 0
)
