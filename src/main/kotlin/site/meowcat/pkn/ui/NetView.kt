package site.meowcat.pkn.ui

import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.scene.Scene
import javafx.scene.Group
import javafx.application.Application
import javafx.stage.Stage

import site.meowcat.pkn.model.NetworkGraph
import kotlin.math.sin
import kotlin.math.cos

import org.pcap4j.core.Pcaps
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.PcapNativeException
import site.meowcat.pkn.capture.startCapture
import kotlin.concurrent.thread

class NetView : Application() {
    override fun start(stage: Stage) {
        val canvas = Canvas(800.0, 800.0)
        val gc = canvas.graphicsContext2D

        val scene = Scene(Group(canvas))
        stage.scene = scene
        stage.title = "NetView - Pickle Network Debugger"
        stage.show()

        val nifs = Pcaps.findAllDevs()
        val nif = nifs.firstOrNull()

        if (nif != null) {
            try {
                val handle = nif.openLive(
                    65536,
                    PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                    10
                )
                thread(isDaemon = true) {
                    startCapture(handle)
                }
            } catch (e: PcapNativeException) {
                System.err.println("Failed to open interface: ${e.message}")
            }
        } else {
            System.err.println("No network interfaces found.")
        }

        Thread {
            while (true) {
                javafx.application.Platform.runLater {
                    draw(gc, canvas.width, canvas.height)
                }
                Thread.sleep(100)
            }
        }.apply { isDaemon = true }.start()
    }

    fun draw(gc: javafx.scene.canvas.GraphicsContext, w: Double, h: Double) {
        gc.fill = Color.BLACK
        gc.fillRect(0.0, 0.0, w, h)

        val nodeList = NetworkGraph.nodes.toList()
        val centerX = w / 2
        val centerY = h / 2
        val radius = 250

        val positions = mutableMapOf<String, Pair<Double, Double>>()

        nodeList.forEachIndexed { index, node ->
            val angle = (index * 2 * Math.PI) / nodeList.size
            val x = centerX + cos(angle) * radius
            var y = centerY + sin(angle) * radius
            positions[node] = x to y

            gc.fill = Color.CYAN
            gc.fillOval(x - 5, y - 5, 10.0, 10.0)

            gc.fill = Color.WHITE
            gc.font = Font.font(10.0)
            gc.textAlign = TextAlignment.CENTER
            gc.fillText(node, x, y - 10)
        }

        gc.stroke = Color.LIME

        NetworkGraph.edges.values.forEach { edge ->
            val a = positions[edge.src] ?: return@forEach
            val b = positions[edge.dst] ?: return@forEach

            gc.lineWidth = (1 + edge.weight.coerceAtMost(10)).toDouble()
            gc.strokeLine(a.first, a.second, b.first, b.second)
        }
    }
}