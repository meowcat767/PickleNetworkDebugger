package site.meowcat.pkn.ui

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.scene.Scene
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Duration
import site.meowcat.pkn.`object`.TrafficStats
import site.meowcat.pkn.model.DeviceStats

class TopTalkersView : Stage() {

    private val table = TableView<DeviceStats>()

    init {
        title = "Top Traffic Devices"

        val ipCol = TableColumn<DeviceStats, String>("IP")
        ipCol.setCellValueFactory { cellData ->
            SimpleStringProperty(cellData.value.ip) as ObservableValue<String>
        }

        val sentCol = TableColumn<DeviceStats, Number>("Sent KB")
        sentCol.setCellValueFactory { cellData ->
            SimpleLongProperty(cellData.value.sentBytes / 1024) as ObservableValue<Number>
        }

        val recvCol = TableColumn<DeviceStats, Number>("Recv KB")
        recvCol.setCellValueFactory { cellData ->
            SimpleLongProperty(cellData.value.recvBytes / 1024) as ObservableValue<Number>
        }

        table.columns.addAll(listOf(ipCol, sentCol, recvCol))

        scene = Scene(VBox(table), 500.0, 400.0)

        Timeline(
            KeyFrame(Duration.seconds(1.0), javafx.event.EventHandler {
                refresh()
            })
        ).apply {
            cycleCount = Animation.INDEFINITE
            play()
        }
    }

    private fun refresh() {
        table.items.setAll(
            TrafficStats.snapshot()
                .sortedByDescending { it.sentBytes + it.recvBytes }
        )
    }
}