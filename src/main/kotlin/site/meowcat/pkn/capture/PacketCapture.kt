package site.meowcat.pkn.capture

import org.pcap4j.core.PcapHandle
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.core.PacketListener
import org.pcap4j.packet.EthernetPacket
import site.meowcat.pkn.model.NetworkGraph

fun startCapture(handle: PcapHandle) {

    handle.loop(-1, PacketListener { packet ->

        val ethernet = packet.get(EthernetPacket::class.java)
        val ip = ethernet?.payload as? IpV4Packet

        val src = ip?.header?.srcAddr?.hostAddress
        val dst = ip?.header?.dstAddr?.hostAddress

        if (src != null && dst != null) {
            NetworkGraph.addFlow(src, dst)
        }
    })
}