package site.meowcat.pkn

import javafx.application.Application

fun main() {
    val process = ProcessBuilder(
        "tshark",
        "-i", "any",
        "-T", "fields",
        "-e", "ip.src",
        "-e", "ip.dst",
        "-e", "_ws.col.Protocol",
        "-c", "10"
    ).start()

    process.inputStream.bufferedReader().forEachLine {
        println(it)
    }
}