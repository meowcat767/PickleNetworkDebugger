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

        // Draw nodes in two lines (left for high weight source, right for destination)
        // For simplicity, let's keep them in one line but make them look better.
        // Actually, "all devices in line" might mean a vertical or horizontal list.
        // Let's try two columns if there are many nodes, or one column centered.
        
        val leftX = w * 0.25
        val rightX = w * 0.75
        
        // Let's just use one column for now as "in line" suggests.
        // But maybe offset them a bit or add some background boxes.
        nodeList.forEachIndexed { index, node ->
            val x = w / 3 // Move to the left a bit to give room for labels
            val y = padding + index * spacing
            positions[node] = x to y

            // Background highlight for the device row
            gc.fill = Color.web("#2d2d2d")
            gc.fillRoundRect(x - 40, y - 25, w * 0.6, 50.0, 15.0, 15.0)

            // Node Circle
            gc.fill = Color.web("#4a9eff")
            gc.fillOval(x - 15, y - 15, 30.0, 30.0)

            // IP/Name Label
            gc.fill = Color.WHITE
            gc.font = Font.font("Monospaced", 14.0)
            gc.textAlign = TextAlignment.LEFT
            val displayName = NetworkGraph.getDisplayName(node)
            gc.fillText(displayName, x + 30, y + 5)
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

        // If it's a vertical-ish line, curve it slightly so return traffic doesn't overlap
        if (Math.abs(x2 - x1) < 1.0) {
            val controlX = x1 + (if (y2 > y1) 40.0 else -40.0)
            val controlY = (y1 + y2) / 2
            gc.beginPath()
            gc.moveTo(sx, sy)
            gc.quadraticCurveTo(controlX, controlY, ex, ey)
            gc.stroke()
            
            // Adjust arrow head position for curve
            // For simplicity, I'll just draw the arrow at the end of the curve
            // The angle at the end of quadratic curve is between (controlX, controlY) and (ex, ey)
            val endAngle = Math.atan2(ey - controlY, ex - controlX)
            drawArrowHead(gc, ex, ey, endAngle, arrowSize)
        } else {
            gc.strokeLine(sx, sy, ex, ey)
            drawArrowHead(gc, ex, ey, angle, arrowSize)
        }
    }

    private fun drawArrowHead(gc: javafx.scene.canvas.GraphicsContext, x: Double, y: Double, angle: Double, size: Double) {
        gc.fill = gc.stroke
        val arrowAngle = Math.PI / 6
        val x1 = x - size * cos(angle - arrowAngle)
        val y1 = y - size * sin(angle - arrowAngle)
        val x2 = x - size * cos(angle + arrowAngle)
        val y2 = y - size * sin(angle + arrowAngle)

        gc.fillPolygon(doubleArrayOf(x, x1, x2), doubleArrayOf(y, y1, y2), 3)
    }
}