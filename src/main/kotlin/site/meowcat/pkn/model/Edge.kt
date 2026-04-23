package site.meowcat.pkn.model

data class Edge(val src: String, val dst: String, var weight: Int = 0)

object NetworkGraph {
    val nodes = mutableSetOf<String>()
    val edges = mutableMapOf<Pair<String, String>, Edge>()
    private val hostNames = mutableMapOf<String, String>()
    private val localSubnets = mutableListOf<java.net.InterfaceAddress>()

    init {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                // Skip loopback, down, and common virtual/VPN interfaces
                if (nif.isLoopback || !nif.isUp) continue
                
                val name = nif.name.lowercase()
                if (name.contains("docker") || name.contains("tailscale") || 
                    name.contains("veth") || name.contains("br-") || name.contains("tun")) continue

                for (addr in nif.interfaceAddresses) {
                    val ip = addr.address
                    if (ip is java.net.Inet4Address) {
                        localSubnets.add(addr)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isLocal(ipStr: String): Boolean {
        if (ipStr == "127.0.0.1") return true
        val target = try {
            java.net.InetAddress.getByName(ipStr).address
        } catch (e: Exception) {
            return false
        }
        
        for (localAddr in localSubnets) {
            val localIp = localAddr.address.address
            val maskLen = localAddr.networkPrefixLength.toInt()
            if (isInSubnet(target, localIp, maskLen)) return true
        }
        return false
    }

    private fun isInSubnet(target: ByteArray, local: ByteArray, maskLen: Int): Boolean {
        if (target.size != local.size) return false
        val bytes = maskLen / 8
        val bits = maskLen % 8
        
        for (i in 0 until bytes) {
            if (target[i] != local[i]) return false
        }
        
        if (bits > 0) {
            val mask = (0xFF shl (8 - bits)).toByte()
            if ((target[bytes].toInt() and mask.toInt()) != (local[bytes].toInt() and mask.toInt())) return false
        }
        
        return true
    }

    @Synchronized
    fun addNode(ip: String) {
        if (!isLocal(ip)) return
        if (nodes.add(ip)) {
            resolveHostName(ip)
        }
    }

    @Synchronized
    fun addFlow(src: String, dst: String) {
        if (!isLocal(src) || !isLocal(dst)) return

        if (nodes.add(src)) resolveHostName(src)
        if (nodes.add(dst)) resolveHostName(dst)

        val key = src to dst
        val edge = edges.getOrPut(key) { Edge(src, dst, 0) }
        edge.weight++
    }

    fun getLocalSubnets(): List<String> {
        return localSubnets.map { addr ->
            val ip = addr.address.hostAddress
            val prefix = addr.networkPrefixLength
            "$ip/$prefix"
        }
    }

    private fun resolveHostName(ip: String) {
        synchronized(this) {
            if (hostNames.containsKey(ip) && hostNames[ip] != ip) return
            // Set a placeholder to avoid multiple lookups for the same IP
            hostNames[ip] = ip
        }

        java.util.concurrent.CompletableFuture.runAsync {
            // 1. Try avahi-resolve-address
            try {
                val process = ProcessBuilder("avahi-resolve-address", ip).start()
                val reader = process.inputStream.bufferedReader()
                val line = reader.readLine()
                if (line != null && line.contains(ip)) {
                    val resolved = line.substringAfter(ip).trim().removeSuffix(".local")
                    if (resolved.isNotEmpty() && resolved != ip) {
                        synchronized(this) { hostNames[ip] = resolved }
                        return@runAsync
                    }
                }
            } catch (e: Exception) {}

            // 2. Try avahi-browse (more intensive, but can find names that resolve-address misses)
            try {
                val process = ProcessBuilder("avahi-browse", "-t", "-r", "-a").start()
                val reader = process.inputStream.bufferedReader()
                var currentAddress: String? = null
                var currentHostname: String? = null
                
                reader.forEachLine { line ->
                    if (line.contains("address = [")) {
                        currentAddress = line.substringAfter("[").substringBefore("]")
                    }
                    if (line.contains("hostname = [")) {
                        currentHostname = line.substringAfter("[").substringBefore("]").removeSuffix(".local")
                    }
                    if (currentAddress == ip && currentHostname != null) {
                        synchronized(this) { 
                            if (hostNames[ip] == ip) {
                                hostNames[ip] = currentHostname!! 
                            }
                        }
                        process.destroy()
                        return@forEachLine
                    }
                }
            } catch (e: Exception) {}

            // 3. Fallback to standard resolution
            try {
                val address = java.net.InetAddress.getByName(ip)
                val hostName = address.hostName
                val canonicalHostName = address.canonicalHostName
                
                val resolvedName = if (hostName != ip) hostName else if (canonicalHostName != ip) canonicalHostName else null
                
                if (resolvedName != null) {
                    synchronized(this) { 
                        if (hostNames[ip] == ip) {
                            hostNames[ip] = resolvedName 
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    @Synchronized
    fun getDisplayName(ip: String): String {
        val hostName = hostNames[ip] ?: ip
        return if (hostName == ip) ip else "$hostName ($ip)"
    }
}