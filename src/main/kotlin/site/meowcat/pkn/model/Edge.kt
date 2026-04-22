package site.meowcat.pkn.model

data class Edge(val src: String, val dst: String, var weight: Int = 0)

val nodes = mutableSetOf<String>()
val edges = mutableMapOf<Pair<String, String>, Edge>()

fun addFlow(src: String, dst: String) {
    nodes.add(src)
    nodes.add(dst)
    
    val key = src to dst
    val edge = edges.getOrPut(key) { Edge(src, dst, 0) }
    edge.weight++
}