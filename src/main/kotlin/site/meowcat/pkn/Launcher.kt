package site.meowcat.pkn

import org.pcap4j.core.Pcaps
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.RawPacketListener

fun main() {
    val nifs = Pcaps.findAllDevs()

    val nif = nifs.firstOrNull() ?: run {
        println("No interfaces found")
        return
    }

    println("Using: ${nif.name}")

    val handle = nif.openLive(
        65536,
        PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
        10
    )

    handle.loop(10, RawPacketListener { packet ->
        println("Packet length: ${packet.size}")
    })

    handle.close()
}