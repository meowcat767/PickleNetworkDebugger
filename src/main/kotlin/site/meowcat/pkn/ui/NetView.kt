package site.meowcat.pkn.ui

import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.scene.Scene
import javafx.scene.Group
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.control.Label
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.application.Application
import javafx.scene.control.Button
import javafx.stage.Stage
import javafx.scene.image.Image

import site.meowcat.pkn.model.NetworkGraph
import kotlin.math.sin
import kotlin.math.cos

import org.pcap4j.core.Pcaps
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.PcapNativeException
import site.meowcat.pkn.capture.startCapture
import site.meowcat.pkn.capture.NetworkScanner
import site.meowcat.pkn.capture.getGateway
import site.meowcat.pkn.capture.isPrivateIp
import kotlin.concurrent.thread

class NetView : Application() {
    private var showEdges = true
    private var filterText = ""

    // generic device names
    private val generic = setOf("android", "Android", "localhost", "none", "device", "\\040none\\041")


    // Zoom and Pan state
    private var scale = 1.0
    private var offsetX = 0.0
    private var offsetY = 0.0
    private var mouseAnchorX = 0.0
    private var mouseAnchorY = 0.0

    override fun start(stage: Stage) {

        val canvas = Canvas(800.0, 800.0)
        val gc = canvas.graphicsContext2D

        val controls = HBox(10.0).apply {
            padding = Insets(10.0)
            alignment = Pos.CENTER_LEFT
            style = "-fx-background-color: #333333;"

            val edgeToggle = CheckBox("Show Arrows").apply {
                isSelected = true
                textFill = Color.WHITE
                setOnAction { showEdges = isSelected }
            }

            val externalToggle = CheckBox("Show External").apply {
                isSelected = false
                textFill = Color.WHITE
                setOnAction { NetworkGraph.showExternalNodes = isSelected }
            }

            val searchField = TextField().apply {
                promptText = "Search devices..."
                textProperty().addListener { _, _, newValue ->
                    filterText = newValue.lowercase()
                }
            }

            val searchLabel = Label("Search:").apply {
                textFill = Color.WHITE
            }

            val statsButton = Button("Traffic Stats").apply {
                setOnAction {
                    TopTalkersView().show()
                }
            }

            children.addAll(edgeToggle, externalToggle, searchLabel, searchField, statsButton)
        }

        val root = VBox(controls, canvas)
        val scene = Scene(root)
        stage.icons.add(
            Image(javaClass.getResourceAsStream("/site/meowcat/pkn/icon.png"))
        )

        // Key events for navigation
        scene.setOnKeyPressed { event ->
            if (!NetworkScanner.isInitialScanComplete) return@setOnKeyPressed
            val step = 50.0 / scale
            when (event.code) {
                javafx.scene.input.KeyCode.UP -> offsetY += step
                javafx.scene.input.KeyCode.DOWN -> offsetY -= step
                javafx.scene.input.KeyCode.LEFT -> offsetX += step
                javafx.scene.input.KeyCode.RIGHT -> offsetX -= step
                else -> {}
            }
        }

        // Bind canvas size to scene size
        canvas.widthProperty().bind(root.widthProperty())
        canvas.heightProperty().bind(root.heightProperty().subtract(controls.heightProperty()))

        // Mouse events for Zoom and Pan
        canvas.setOnMousePressed { event ->
            if (!NetworkScanner.isInitialScanComplete) return@setOnMousePressed
            mouseAnchorX = event.x
            mouseAnchorY = event.y
        }

        canvas.setOnMouseDragged { event ->
            if (!NetworkScanner.isInitialScanComplete) return@setOnMouseDragged
            offsetX += (event.x - mouseAnchorX)
            offsetY += (event.y - mouseAnchorY)
            mouseAnchorX = event.x
            mouseAnchorY = event.y
        }

        canvas.setOnScroll { event ->
            if (!NetworkScanner.isInitialScanComplete) return@setOnScroll
            val delta = event.deltaY
            if (delta == 0.0) return@setOnScroll

            // Zoom towards mouse position
            val mouseX = event.x
            val mouseY = event.y

            val relX = (mouseX - offsetX) / scale
            val relY = (mouseY - offsetY) / scale

            val zoomFactor = if (delta > 0) 1.1 else 1.0 / 1.1
            val newScale = (scale * zoomFactor).coerceIn(0.1, 10.0)

            if (newScale != scale) {
                scale = newScale
                offsetX = mouseX - relX * scale
                offsetY = mouseY - relY * scale
            }

            event.consume()
        }

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
                NetworkScanner.startScanning()
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

        gc.save()
        gc.translate(offsetX, offsetY)
        gc.scale(scale, scale)

        val filteredNodes = NetworkGraph.nodes.filter { ip ->
            val displayName = prettyName(ip).lowercase()
            filterText.isEmpty() || displayName.contains(filterText) || ip.contains(filterText)
        }.sorted()

        val router = NetworkGraph.getGateway()

        val positions = mutableMapOf<String, Pair<Double, Double>>()
        val centerX = w / 2
        val centerY = h / 2

        val localNodes = filteredNodes.filter { NetworkGraph.isLocalNode(it) }
        val externalNodes = filteredNodes.filter { !NetworkGraph.isLocalNode(it) }

        if (router != null && router in localNodes) {
            positions[router] = centerX to centerY
        }

        val others = localNodes.filter { it != router }
        var currentRadius = 300.0
        val ringSpacing = 150.0
        val nodesPerRing = 20

        if (others.isNotEmpty()) {
            others.chunked(nodesPerRing).forEach { ringNodes ->
                ringNodes.forEachIndexed { index, node ->
                    val angle = (2 * Math.PI * index) / ringNodes.size
                    val x = centerX + cos(angle) * currentRadius
                    val y = centerY + sin(angle) * currentRadius
                    positions[node] = x to y
                }
                currentRadius += ringSpacing
            }
        }

        if (externalNodes.isNotEmpty()) {
            // Start external rings further out
            currentRadius += ringSpacing 
            externalNodes.chunked(nodesPerRing).forEach { ringNodes ->
                ringNodes.forEachIndexed { index, node ->
                    val angle = (2 * Math.PI * index) / ringNodes.size
                    val x = centerX + cos(angle) * currentRadius
                    val y = centerY + sin(angle) * currentRadius
                    positions[node] = x to y
                }
                currentRadius += ringSpacing
            }
        }

        filteredNodes.forEach { node ->
            val pos = positions[node] ?: return@forEach
            val x = pos.first
            val y = pos.second
            
            val isLocal = NetworkGraph.isLocalNode(node)

            // Background highlight for the device cell
            gc.fill = if (isLocal) Color.web("#2d2d2d") else Color.web("#3d2d2d")
            gc.fillRoundRect(x - 70, y - 40, 140.0, 80.0, 15.0, 15.0)

            // Node Circle
            gc.fill = if (isLocal) Color.web("#4a9eff") else Color.web("#ff4a4a")
            gc.fillOval(x - 15, y - 15, 30.0, 30.0)

            // IP/Name Label
            gc.fill = Color.WHITE
            gc.font = Font.font("Monospaced", 10.0)
            gc.textAlign = TextAlignment.CENTER
            val displayName = prettyName(node)

            // Split display name if too long
            if (displayName.contains(" (")) {
                val parts = displayName.split(" (")
                gc.fillText(parts[0], x, y + 30)
                gc.fillText("(" + parts[1], x, y + 45)
            } else {
                gc.fillText(displayName, x, y + 30)
            }
        }

        // Draw edges as arrows (flashing for recent activity)
        if (showEdges) {
            val now = System.currentTimeMillis()
            NetworkGraph.edges.values.forEach { edge ->
                val timeSinceLastPacket = now - edge.lastPacketTime
                if (timeSinceLastPacket > 1000) return@forEach // Only flash for 1 second

                val start = positions[edge.src] ?: return@forEach
                val end = positions[edge.dst] ?: return@forEach

                if (edge.src == edge.dst) return@forEach

                // Fade out based on time
                val opacity = (1.0 - (timeSinceLastPacket / 1000.0)).coerceIn(0.0, 1.0)
                gc.stroke = Color.web("#00ff00", opacity * 0.8)
                gc.lineWidth = 2.0

                drawArrow(gc, start.first, start.second, end.first, end.second, edge.lastRequest, opacity)
            }
        }
        gc.restore()

        // Draw overlay if scanning
        if (!NetworkScanner.isInitialScanComplete) {
            gc.fill = Color.web("#000000", 0.7)
            gc.fillRect(0.0, 0.0, w, h)

            gc.fill = Color.WHITE
            gc.font = Font.font("Monospaced", 24.0)
            gc.textAlign = TextAlignment.CENTER
            gc.fillText("Probing Network...", w / 2, h / 2)

            gc.font = Font.font("Monospaced", 14.0)
            gc.fillText("Please wait for the initial discovery to complete", w / 2, h / 2 + 40)
        }
    }

    private fun drawArrow(gc: javafx.scene.canvas.GraphicsContext, x1: Double, y1: Double, x2: Double, y2: Double, label: String? = null, opacity: Double = 1.0) {
        val arrowSize = 10.0
        val angle = Math.atan2(y2 - y1, x2 - x1)

        // Offset start and end points to be outside the node circles
        val offset = 15.0
        val sx = x1 + offset * cos(angle)
        val sy = y1 + offset * sin(angle)
        val ex = x2 - offset * cos(angle)
        val ey = y2 - offset * sin(angle)

        val midX = (sx + ex) / 2
        val midY = (sy + ey) / 2

        // Offset control point perpendicular to the line
        val dx = ex - sx
        val dy = ey - sy
        val len = Math.sqrt(dx * dx + dy * dy).coerceAtLeast(1.0)
        val nx = -dy / len
        val ny = dx / len

        val curveAmount = 30.0
        val controlX = midX + nx * curveAmount
        val controlY = midY + ny * curveAmount

        gc.beginPath()
        gc.moveTo(sx, sy)
        gc.quadraticCurveTo(controlX, controlY, ex, ey)
        gc.stroke()

        // Draw label along the curve (at t=0.5)
        if (label != null) {
            val tx = 0.25 * sx + 0.5 * controlX + 0.25 * ex
            val ty = 0.25 * sy + 0.5 * controlY + 0.25 * ey

            gc.save()
            gc.fill = Color.web("#00ff00", opacity)
            gc.font = Font.font("Monospaced", 10.0)
            gc.textAlign = TextAlignment.CENTER

            gc.fillText(label, tx, ty - 5)
            gc.restore()
        }

        val endAngle = Math.atan2(ey - controlY, ex - controlX)
        drawArrowHead(gc, ex, ey, endAngle, arrowSize)
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

    fun prettyName(ip: String): String {
        val name = NetworkGraph.getDetectedName(ip)

        return if (name.lowercase() in generic) {
            "$name, unknown ($ip)"
        } else {
            "$name ($ip)"
        }
    }
}