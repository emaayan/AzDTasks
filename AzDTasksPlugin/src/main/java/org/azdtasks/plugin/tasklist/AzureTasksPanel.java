package org.azdtasks.plugin.tasklist;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.Task;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.azdtasks.plugin.AzDoRepository;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AzureTasksPanel extends JBPanel<AzureTasksPanel> {

    private final Project project;
    private final TaskTableModel tableModel = new TaskTableModel();
    private String lastQuery="";
    private ScheduledExecutorService scheduler;
    private static final int REFRESH_INTERVAL_SEC = 30;

    static class TaskTableModel extends AbstractTableModel {

        public record Column<T>(String name, Class<T> cls, Function<Task,T> getter,int width){};
        public static final List<Column> l=List.of(
                new Column<>("Type", String.class, task -> task.getType().name(),10)
                ,new Column<>("Id", Integer.class, task -> Integer.valueOf(task.getNumber()),10)
                ,new Column<>("Summary", String.class, Task::getSummary,300)
                ,new Column<>("Updated", Date.class, Task::getUpdated,120)
        );

        private List<Task> tasks = new ArrayList<>();
        public void setTasks(List  tasks) {
            this.tasks = tasks;
            fireTableDataChanged();

        }

        @Override public int getRowCount()    { return tasks.size(); }
        @Override public int getColumnCount() { return l.size(); }
        @Override public String getColumnName(int col) { return l.get(col).name(); }

        @Override
        public Object getValueAt(int row, int col) {
            final Task task = tasks.get(row);
            return l.get(col).getter().apply(task);
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return l.get(col).cls();
        }
        public void setColWith(TableColumnModel tbc){
            for (int i = 0; i < getColumnCount(); i++) {
                Column column = l.get(i);
                tbc.getColumn(i).setPreferredWidth(column.width());
            }
        }
        public Task getTask(int modelRow) {
            return tasks.get(modelRow);
        }
    }

    public AzureTasksPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;

        final JBTable table = new JBTable(tableModel);

        final TableRowSorter<TaskTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        final JBTextField searchField = new JBTextField();
        final Timer debounceTimer = new Timer(500, e -> loadTasks(searchField.getText()));
        debounceTimer.setRepeats(false);
        searchField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                debounceTimer.restart();
//                final String text = searchField.getText();
//                sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text));
            }
        });

        add(searchField, BorderLayout.NORTH);

        final TableColumnModel columnModel = table.getColumnModel();
        tableModel.setColWith(columnModel);
        table.setShowGrid(false);
        table.setStriped(true); // IntelliJ alternating row colors

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row < 0) return;

                    // Account for sorting — convert view row to model row
                    final int modelRow = table.convertRowIndexToModel(row);
                    final Task task = tableModel.getTask(modelRow);

                    final String url = task.getIssueUrl();
                    if (StringUtil.isNotEmpty(url)) {
                        BrowserUtil.browse(url);
                    }
                }
            }
        });
        add(new JBScrollPane(table), BorderLayout.CENTER);


        loadTasks("Test Defect"); // load on init
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        scheduler = AppExecutorUtil.createBoundedScheduledExecutorService("AzureTasksRefresh", 1);
        scheduler.scheduleWithFixedDelay(() -> loadTasks(lastQuery),REFRESH_INTERVAL_SEC,REFRESH_INTERVAL_SEC,TimeUnit.SECONDS);
    }

    public void dispose() {
        stopAutoRefresh();
        //if (countdownTimer != null) countdownTimer.stop();
    }

    private void stopAutoRefresh() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
//    private void resetCountdown() {
//        secondsUntilRefresh = REFRESH_INTERVAL_SEC;
//        ApplicationManager.getApplication().invokeLater(() -> {
//            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
//            statusLabel.setText("Last refreshed: " + LocalTime.now().format(fmt) + "  ");
//        });
//    }

    private void loadTasks(String query) {
        // Run off EDT to avoid blocking UI
        lastQuery = query; // remember for auto-refresh
        ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Backgroundable(project, "Loading tasks") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        AzureTasksToolWindowFactory.get(project).ifPresent(azureRepo -> {
                            try {
                                if (StringUtil.isNotEmpty(query)) {
                                    final Task[] tasks = getIssues(indicator, azureRepo, query);
                                    // Update UI on EDT
                                    ApplicationManager.getApplication().invokeLater(() -> extracted(tasks));
                                }
                            } catch (Exception e) {
                                ApplicationManager.getApplication().invokeLater(() ->
                                        Messages.showErrorDialog(project, e.getMessage(), "Tasks Error")
                                );
                            }
                        });
                    }
                }
        );
    }

    private static Task[] getIssues(ProgressIndicator indicator, AzDoRepository azureRepo, String query) throws Exception {
        return azureRepo.getIssues(query, 0, 100, false, indicator);
    }

    private void extracted(Task[] tasks) {
        tableModel.setTasks(Arrays.asList(tasks));
    }
}