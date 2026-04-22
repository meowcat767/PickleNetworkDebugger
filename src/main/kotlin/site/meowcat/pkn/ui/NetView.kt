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
        gc.fill = Color.web("#1e1e1e")
        gc.fillRect(0.0, 0.0, w, h)

        val nodeList = NetworkGraph.nodes.toList().sorted()
        val positions = mutableMapOf<String, Pair<Double, Double>>()

        val padding = 100.0
        val availableHeight = h - 2 * padding
        val spacing = if (nodeList.size > 1) availableHeight / (nodeList.size - 1) else 0.0

        // Draw nodes in a line (vertical)
        nodeList.forEachIndexed { index, node ->
            val x = w / 2
            val y = padding + index * spacing
            positions[node] = x to y

            // Node Circle
            gc.fill = Color.web("#4a9eff")
            gc.fillOval(x - 15, y - 15, 30.0, 30.0)

            // IP/Name Label
            gc.fill = Color.WHITE
            gc.font = Font.font("Monospaced", 12.0)
            gc.textAlign = TextAlignment.LEFT
            val displayName = NetworkGraph.getDisplayName(node)
            gc.fillText(displayName, x + 25, y + 5)
        }

        // Draw edges as arrows
        NetworkGraph.edges.values.forEach { edge ->
            val start = positions[edge.src] ?: return@forEach
            val end = positions[edge.dst] ?: return@forEach

            if (edge.src == edge.dst) return@forEach // Skip self-loops for now or draw differently

            gc.stroke = Color.web("#00ff00", 0.6)
            gc.lineWidth = (1 + edge.weight.coerceAtMost(5)).toDouble()

            drawArrow(gc, start.first, start.second, end.first, end.second)
        }
    }

    private fun drawArrow(gc: javafx.scene.canvas.GraphicsContext, x1: Double, y1: Double, x2: Double, y2: Double) {
        val arrowSize = 10.0
        val angle = Math.atan2(y2 - y1, x2 - x1)

        // Offset start and end points to be outside the node circles
        val offset = 15.0
        val sx = x1 + offset * cos(angle)
        val sy = y1 + offset * sin(angle)
        val ex = x2 - offset * cos(angle)
        val ey = y2 - offset * sin(angle)

        gc.strokeLine(sx, sy, ex, ey)

        // Arrow head
        gc.fill = gc.stroke
        val arrowAngle = Math.PI / 6
        val x3 = ex - arrowSize * cos(angle - arrowAngle)
        val y3 = ey - arrowSize * sin(angle - arrowAngle)
        val x4 = ex - arrowSize * cos(angle + arrowAngle)
        val y4 = ey - arrowSize * sin(angle + arrowAngle)

        gc.fillPolygon(doubleArrayOf(ex, x3, x4), doubleArrayOf(ey, y3, y4), 3)
    }
}