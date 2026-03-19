package org.tasklist.plugin.table;

import com.intellij.util.ui.JBUI;

import javax.swing.*;

public class IconData {
    private final Icon icon;
    private final String text;
    private int hAlign;

    public IconData(Icon icon) {
        this(icon, "", SwingConstants.CENTER);
    }

    public IconData(Icon icon, String text) {
        this(icon, text, SwingConstants.LEFT);
    }

    public IconData(Icon icon, String text, int hAlign) {
        this.icon = icon;
        this.text = text;
        this.hAlign = hAlign;
    }

    public void update(JLabel label) {
        label.setText(text);
        label.setIcon(icon);
        label.setHorizontalAlignment(hAlign);
        label.setBorder(JBUI.Borders.empty());
    }

}
