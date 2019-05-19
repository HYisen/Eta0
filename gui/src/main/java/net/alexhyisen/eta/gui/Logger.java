package net.alexhyisen.eta.gui;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.stage.PopupWindow;
import net.alexhyisen.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2016/11/26.
 * Logger is purposed to show the logging message.
 */
class Logger {
    private Label label;
    private ContextMenu cm;
    private List<String> data = new ArrayList<>();
    //Lacking in size control, data will expand without limit.

    Logger(Label label) {
        this.label = label;
        label.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMsgLabelAction);
    }

    void push(String msg) {
        Utility.log(msg);
        data.add(msg);
        label.setText(msg);
    }

    private void handleMsgLabelAction(MouseEvent e) {
        Utility.log("label clicked");
        if (cm != null && cm.isShowing()) {
            cm.hide();
        } else {
            cm = new ContextMenu();
            List<MenuItem> items = Utility.revRange(data.size(), 0)
                    .limit(10)
                    .mapToObj(v -> data.get(v))
                    .map(MenuItem::new)
                    .collect(Collectors.toList());
            Collections.reverse(items);
            cm.getItems().addAll(items);
            cm.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_LEFT);
            cm.show(label, e.getSceneX() + 380, e.getSceneY() + 140 - 23 * cm.getItems().size());
            //cm.show(label,label.getLayoutX()+400,label.getLayoutY()+40-23*cm.getItems().size());
        }
    }
}