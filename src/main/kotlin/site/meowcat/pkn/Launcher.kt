package site.meowcat.pkn

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.TextArea
import javafx.stage.Stage
import kotlin.concurrent.thread

class App : Application() {

    override fun start(stage: Stage) {
        val output = TextArea()
        output.isEditable = false

        stage.title = "Pickle Network Debugger"
        stage.scene = Scene(output, 800.0, 500.0)
        stage.show()

        thread {
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
                javafx.application.Platform.runLater {
                    output.appendText("$it\n")
                }
            }
        }
    }
}

fun main() {
    Application.launch(App::class.java)
}