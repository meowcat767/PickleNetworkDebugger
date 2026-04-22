package site.meowcat.pkn.model

data class Edge(val src: String, val dst: String, var weight: Int = 0)

object NetworkGraph {
    val nodes = mutableSetOf<String>()
    val edges = mutableMapOf<Pair<String, String>, Edge>()
    private val hostNames = mutableMapOf<String, String>()
    private val localSubnets = mutableListOf<String>()

    init {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (nif.isLoopback || !nif.isUp) continue
                for (addr in nif.interfaceAddresses) {
                    val ip = addr.address
                    if (ip is java.net.Inet4Address) {
                        val network = "${ip.hostAddress.substringBeforeLast(".")}.0"
                        localSubnets.add(network)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isLocal(ip: String): Boolean {
        if (ip == "127.0.0.1") return true
        val network = "${ip.substringBeforeLast(".")}.0"
        return localSubnets.contains(network)
    }

    @Synchronized
    fun addNode(ip: String) {
        if (!isLocal(ip)) return
        nodes.add(ip)
        resolveHostName(ip)
    }

    @Synchronized
    fun addFlow(src: String, dst: String) {
        if (!isLocal(src) || !isLocal(dst)) return

        nodes.add(src)
        nodes.add(dst)

        val key = src to dst
        val edge = edges.getOrPut(key) { Edge(src, dst, 0) }
        edge.weight++

        resolveHostName(src)
        resolveHostName(dst)
    }

    fun getLocalSubnets(): List<String> = localSubnets.toList()

    private fun resolveHostName(ip: String) {
        if (hostNames.containsKey(ip)) return

        // Set a placeholder to avoid multiple lookups for the same IP
        hostNames[ip] = ip

        java.util.concurrent.CompletableFuture.runAsync {
            try {
                // Try avahi-resolve-address first for friendly mDNS names
                val process = ProcessBuilder("avahi-resolve-address", ip).start()
                val reader = process.inputStream.bufferedReader()
                val line = reader.readLine()
                if (line != null && line.contains(ip)) {
                    val resolved = line.substringAfter(ip).trim().removeSuffix(".local")
                    if (resolved.isNotEmpty()) {
                        synchronized(this) {
                            hostNames[ip] = resolved
                        }
                        return@runAsync
                    }
                }
            } catch (e: Exception) {
                // Avahi not available or failed, fallback to standard resolution
            }

            try {
                val hostName = java.net.InetAddress.getByName(ip).hostName
                synchronized(this) {
                    hostNames[ip] = hostName
                }
            } catch (e: Exception) {
                // Keep the IP as host name if resolution fails
            }
        }
    }

    @Synchronized
    fun getDisplayName(ip: String): String {
        val hostName = hostNames[ip] ?: ip
        return if (hostName == ip) ip else "$hostName ($ip)"
    }
}