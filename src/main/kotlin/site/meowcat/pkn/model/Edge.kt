package site.meowcat.pkn.model

data class Edge(val src: String, val dst: String, var weight: Int = 0)

object NetworkGraph {
    val nodes = mutableSetOf<String>()
    val edges = mutableMapOf<Pair<String, String>, Edge>()
    private val hostNames = mutableMapOf<String, String>()

    @Synchronized
    fun addFlow(src: String, dst: String) {
        nodes.add(src)
        nodes.add(dst)

        val key = src to dst
        val edge = edges.getOrPut(key) { Edge(src, dst, 0) }
        edge.weight++

        resolveHostName(src)
        resolveHostName(dst)
    }

    private fun resolveHostName(ip: String) {
        if (hostNames.containsKey(ip)) return

        // Set a placeholder to avoid multiple lookups for the same IP
        hostNames[ip] = ip

        java.util.concurrent.CompletableFuture.runAsync {
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