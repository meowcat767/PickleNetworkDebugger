package site.meowcat.pkn.capture

import site.meowcat.pkn.model.NetworkGraph
import java.net.InetAddress
import kotlin.concurrent.thread

object NetworkScanner {
    var isInitialScanComplete = false
        private set

    fun startScanning() {
        thread(isDaemon = true) {
            var firstScan = true
            while (true) {
                val subnets = NetworkGraph.getLocalSubnets()
                for (subnet in subnets) {
                    scanSubnet(subnet)
                }
                if (firstScan) {
                    isInitialScanComplete = true
                    firstScan = false
                }
                Thread.sleep(60000) // Scan every minute
            }
        }
    }

    private fun scanSubnet(subnet: String) {
        val prefix = subnet.substringBeforeLast(".")
        val executor = java.util.concurrent.Executors.newFixedThreadPool(20)
        for (i in 1..254) {
            val ip = "$prefix.$i"
            executor.execute {
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(1000)) {
                        NetworkGraph.addNode(ip)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)
    }
}
