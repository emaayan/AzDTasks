package org.tasklist.plugin.table;

import javax.swing.table.TableCellRenderer;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ColumnRenderer<T, R> {
    private final String name;
    private final Class<R> cls;
    private final Function<T, R> getter;
    private final int width;
    private final TableCellRenderer tableCellRenderer;

    public ColumnRenderer(String name, Class<R> cls, Function<T, R> getter, int width) {
        this(name, cls, getter, width, null);
    }

    public ColumnRenderer(String name, Class<R> cls, Function<T, R> getter, int width, final TableCellRenderer tableCellRenderer) {
        this.name = name;
        this.cls = cls;
        this.getter = getter;
        this.width = width;
        this.tableCellRenderer = tableCellRenderer;
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

    public Optional<TableCellRenderer> getTableCellRenderer() {
        return Optional.ofNullable(tableCellRenderer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final ColumnRenderer<T, ?> that = (ColumnRenderer<T, ?>) obj;
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
