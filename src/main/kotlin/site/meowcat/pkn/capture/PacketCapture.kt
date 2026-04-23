package site.meowcat.pkn.capture

import org.pcap4j.core.PcapHandle
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.core.PacketListener
import org.pcap4j.packet.EthernetPacket
import org.pcap4j.packet.TcpPacket
import org.pcap4j.packet.UdpPacket
import site.meowcat.pkn.model.NetworkGraph

fun startCapture(handle: PcapHandle) {

    handle.loop(-1, PacketListener { packet ->

        val ethernet = packet.get(EthernetPacket::class.java)
        val ip = ethernet?.payload as? IpV4Packet

        val src = ip?.header?.srcAddr?.hostAddress
        val dst = ip?.header?.dstAddr?.hostAddress

        if (src != null && dst != null) {
            var requestInfo: String? = null

            // Try to extract some useful "request" info
            val tcp = ip.payload as? TcpPacket
            val udp = ip.payload as? UdpPacket

            if (tcp != null && tcp.payload != null) {
                val data = String(tcp.payload.rawData)
                if (data.startsWith("GET ") || data.startsWith("POST ") || data.startsWith("HTTP/")) {
                    val hostLine = data.lines().find { it.startsWith("Host: ") }
                    requestInfo = hostLine?.substringAfter("Host: ")?.trim() ?: "HTTP Request"
                }
            } else if (udp != null && (udp.header.dstPort.valueAsInt() == 53 || udp.header.srcPort.valueAsInt() == 53)) {
                requestInfo = "DNS Query"
            }

            // Fallback to destination name/IP if no protocol-specific info found
            if (requestInfo == null) {
                requestInfo = NetworkGraph.getDisplayName(dst)
                if (requestInfo.contains(" (")) {
                    requestInfo = requestInfo.substringBefore(" (")
                }
            }

            // If the request info is still just the gateway's IP or name, it's not very helpful as a label
            val gateway = NetworkGraph.getGateway()
            if (requestInfo == gateway || (gateway != null && NetworkGraph.getDisplayName(gateway).startsWith(requestInfo!!))) {
                requestInfo = null
            }

            NetworkGraph.addFlow(src, dst, requestInfo)
        }
    })
}