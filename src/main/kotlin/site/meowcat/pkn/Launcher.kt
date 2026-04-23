package site.meowcat.pkn
 
import javafx.application.Application
import site.meowcat.pkn.ui.NetView
import site.meowcat.pkn.capture.getGateway

fun main(args: Array<String>) {
    val gateway = getGateway()
    println("Router: $gateway")
    Application.launch(NetView::class.java, *args)
}
