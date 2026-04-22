package site.meowcat.pkn

import org.pcap4j.core.PacketListener
import org.pcap4j.core.PcapNativeException
import org.pcap4j.core.Pcaps
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.RawPacketListener
import org.pcap4j.packet.EthernetPacket
import org.pcap4j.packet.IpV4Packet
import site.meowcat.pkn.model.addFlow
import site.meowcat.pkn.model.edges

fun main() {
    val nifs = Pcaps.findAllDevs()

    val nif = nifs.firstOrNull() ?: run {
        println("No interfaces found")
        return
    }

    println("Using: ${nif.name}")

    val handle = try {
        nif.openLive(
            65536,
            PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
            10
        )
    } catch (e: PcapNativeException) {
        if (e.message?.contains("permission", ignoreCase = true) == true) {
            System.err.println("\nERROR: Permission denied while opening interface ${nif.name}")
            System.err.println("To fix this, you can either:")
            System.err.println("1. Run the application with sudo.")
            System.err.println("2. Grant CAP_NET_RAW and CAP_NET_ADMIN capabilities to your Java binary:")
            System.err.println("   sudo setcap cap_net_raw,cap_net_admin=eip \$(readlink -f \$(which java))")
        } else {
            System.err.println("Native error: ${e.message}")
        }
        return
    }
    handle.loop(-1, PacketListener { packet ->

        val ethernet = packet.get(EthernetPacket::class.java)
        val ip = ethernet?.payload as? IpV4Packet

        val src = ip?.header?.srcAddr?.hostAddress
        val dst = ip?.header?.dstAddr?.hostAddress

        if (src != null && dst != null) {
            addFlow(src, dst)
            val edge = edges[src to dst]
            println("$src -> $dst (weight: ${edge?.weight}, length: ${packet.length()})")
        }
    })

        handle.close()
    }
