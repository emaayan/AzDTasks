package org.tasklist.plugin;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.util.*;
import java.util.function.Function;

public class BoundTableModel<T> extends AbstractTableModel {


    public class Column<R> {
        private final String name;
        private final Class<R> cls;
        private final Function<T, R> getter;
        private final int width;

        public Column(String name, Class<R> cls, Function<T, R> getter, int width) {
            this.name = name;
            this.cls = cls;
            this.getter = getter;
            this.width = width;
        }

        public String name() {
            return name;
        }

        public Class<R> cls() {
            return cls;
        }

        public Function<T, R> getter() {
            return getter;

        }

        public int width() {
            return width;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            Column<?> that = (Column<?>) obj;
            return Objects.equals(this.name, that.name) &&
                    Objects.equals(this.cls, that.cls) &&
                    Objects.equals(this.getter, that.getter) &&
                    this.width == that.width;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, cls, getter, width);
        }

        @Override
        public String toString() {
            return "Column[" +
                    "name=" + name + ", " +
                    "cls=" + cls + ", " +
                    "getter=" + getter + ", " +
                    "width=" + width + ']';
        }

    }

    private final List<Column<?>> l = new ArrayList<>();
    private List<T> objectList = new ArrayList<>();

    public BoundTableModel(Column<?>... cols) {
        l.addAll(Arrays.asList(cols));
    }

    private final TableRowSorter<BoundTableModel<T>> sorter = new TableRowSorter<>(this);

    public BoundTableModel<T> add(Column<?>... cols) {
        l.addAll(Arrays.asList(cols));
        return this;
    }

    public JBTable createTable() {
        final JBTable table = new JBTable(this);
        setColWith(table.getColumnModel());
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setStriped(true); // IntelliJ alternating row colors
        return table;
    }

    @Override
    public int getRowCount() {
        return objectList.size();
    }


    @Override
    public int getColumnCount() {
        return l.size();
    }

    public void filter(String text) {
        sorter.setRowFilter(StringUtil.isNotEmpty(text) ? RowFilter.regexFilter("(?i)" + text) : null);
    }

    @Override
    public String getColumnName(int col) {
        return l.get(col).name();
    }

    @Override
    public Object getValueAt(int row, int col) {
        final T o = objectList.get(row);
        return l.get(col).getter().apply(o);
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return l.get(col).cls();
    }

    public void setColWith(TableColumnModel tbc) {
        for (int i = 0; i < getColumnCount(); i++) {
            final Column<?> column = l.get(i);
            tbc.getColumn(i).setPreferredWidth(column.width());
        }
    }

    public void set(List<T> objectList) {
        this.objectList = objectList;
        fireTableDataChanged();
    }

    public Optional<T> get(int modelRow) {
        if (modelRow >= 0 && modelRow < objectList.size()) {
            return Optional.of(objectList.get(modelRow));
        } else {
            return Optional.empty();
        }
    }

    public Optional<T> getSelected(final JBTable table) {
        final int viewRow = table.getSelectedRow();
        if (viewRow < 0) return Optional.empty();
        final int modelRow = table.convertRowIndexToModel(viewRow);
        return get(modelRow);
    }

}
