package site.meowcat.pkn.capture

fun getGateway(): String? {
    val process = ProcessBuilder("ip", "route").start()
    val output = process.inputStream.bufferedReader().readText()

    return output.lineSequence()
        .firstOrNull { it.contains("default via")} ?.split(" ") ?.getOrNull(2)
}