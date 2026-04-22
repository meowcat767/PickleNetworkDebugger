package site.meowcat.pkn.ui

import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.Scene
import javafx.scene.Group
import javafx.application.Application
import javafx.stage.Stage

import site.meowcat.pkn.model.nodes
import site.meowcat.pkn.model.edges
import kotlin.math.sin
import kotlin.math.cos

class NetView : Application() {
    override fun start(stage: Stage) {
        val canvas = Canvas(800.0, 800.0)
        val gc = canvas.graphicsContext2D

        val scene = Scene(Group(canvas))
        stage.scene = scene
        stage.title = "NetView - Pickle Network Debugger"
        stage.show()

        Thread {
            while (true) {
                javafx.application.Platform.runLater {
                    draw(gc, canvas.width, canvas.height)
                }
            }
        }.start()
    }

    fun draw(gc: javafx.scene.canvas.GraphicsContext, w: Double, h: Double) {
        gc.fill = Color.BLACK
        gc.fillRect(0.0, 0.0, w, h)

        val nodeList = nodes.toList()
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
        }

        gc.stroke = Color.LIME

        edges.values.forEach { edge ->
            val a = positions[edge.src] ?: return@forEach
            val b = positions[edge.dst] ?: return@forEach

            gc.lineWidth = (1 + edge.weight.coerceAtMost(10)).toDouble()
            gc.strokeLine(a.first, a.second, b.first, b.second)
        }
    }
}