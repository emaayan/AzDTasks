package org.tasklist.plugin.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.function.Function;

public class IconColumnRender<T> extends ColumnRenderer<T, IconData> {

    public IconColumnRender(String name, Function<T, IconData> getter) {
        super(name, IconData.class, getter, 10, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                if (value instanceof IconData iconData) {
                    iconData.update(label);
                }
                return label;
            }
        });
    }
}
