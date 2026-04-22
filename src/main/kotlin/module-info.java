module site.meowcat.pkn {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;


    opens site.meowcat.pkn to javafx.fxml;
    exports site.meowcat.pkn;
}