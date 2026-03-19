package org.tasklist.plugin.table;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class BoundTableModel<T> extends AbstractTableModel {


    private final List<ColumnRenderer<T, ?>> l = new ArrayList<>();
    private List<T> objectList = new ArrayList<>();


    private final TableRowSorter<BoundTableModel<T>> sorter = new TableRowSorter<>(this);

   // private final JBTable table;

    public BoundTableModel() {
     //   table = createTable();
    }

    public @NotNull JBTable createTable() {
        final JBTable table = new JBTable(this);
        setColWith(table.getColumnModel());
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setStriped(true); // IntelliJ alternating row colors
        return table;
    }

    public BoundTableModel<T> add(ColumnRenderer<T, ?>... cols) {
        l.addAll(Arrays.asList(cols));
        return this;
    }

//    public JBTable getTable() {
//        return table;
//    }

    public void filter(String text) {
        sorter.setRowFilter(StringUtil.isNotEmpty(text) ? RowFilter.regexFilter("(?i)" + text) : null);
    }

    @Override
    public int getRowCount() {
        return objectList.size();
    }

    @Override
    public int getColumnCount() {
        return l.size();
    }

    @Override
    public String getColumnName(int col) {
        return l.get(col).name();
    }

    @Override
    public Object getValueAt(int row, int col) {
        final T o = objectList.get(row);
        final ColumnRenderer<T, ?> columnRenderer = l.get(col);
        return columnRenderer.getter().apply(o);
    }

    @Override
    public Class<?> getColumnClass(int col) {
        final ColumnRenderer<T, ?> tColumnRenderer = l.get(col);
        return tColumnRenderer.cls();
    }


    protected void setColWith(TableColumnModel tbc) {
        for (int i = 0; i < getColumnCount(); i++) {
            final ColumnRenderer<T, ?> columnRenderer = l.get(i);
            final TableColumn tableColumn = tbc.getColumn(i);
            columnRenderer.getTableCellRenderer().ifPresent(tableColumn::setCellRenderer);
            tableColumn.setPreferredWidth(columnRenderer.width());

        }
    }

    public void set(List<T> objectList,JBTable table) {
        Optional<T> selected = getSelected(table);
        this.objectList = objectList;
        fireTableDataChanged();
        selected.ifPresent(t -> {
            for (int i = 0; i < objectList.size(); i++) {
                final T t1 = objectList.get(i);
                if (t1.equals(t)){
                    final int viewRow = table.convertRowIndexToView(i);
                    if (viewRow >= 0) {
                        table.setRowSelectionInterval(viewRow, viewRow);
                        table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                    }
                    return;
                }
            }
        });

    }

    public Optional<T> get(int modelRow) {
        if (modelRow >= 0 && modelRow < objectList.size()) {
            return Optional.of(objectList.get(modelRow));
        } else {
            return Optional.empty();
        }
    }

    public Optional<T> getSelected(JBTable table) {
        final int viewRow = table.getSelectedRow();
        if (viewRow < 0) return Optional.empty();
        final int modelRow = table.convertRowIndexToModel(viewRow);
        return get(modelRow);
    }

}
