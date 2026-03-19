package org.tasklist.plugin;


import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.actions.OpenTaskDialog;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.tasklist.plugin.table.BoundTableModel;
import org.tasklist.plugin.table.ColumnRenderer;
import org.tasklist.plugin.table.IconColumnRender;
import org.tasklist.plugin.table.IconData;


import javax.swing.*;
import javax.swing.event.DocumentEvent;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class AbstractTasksPanel<T extends Task> extends JBPanel<AbstractTasksPanel<? extends Task>> {

    private final static Logger LOG = Logger.getInstance(AbstractTasksPanel.class);

    private final Project project;

    private final BoundTableModel<T> taskTableModel = new BoundTableModel<>();
    private final TaskDetailPanel<T> detailPanel = new TaskDetailPanel<>();
    private String lastQuery = "";

    private static final String REFRESH_INTERVAL_KEY = "AzureTasks.refreshInterval";
    private static final int DEFAULT_REFRESH_INTERVAL = 30;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture = null;

    private final JBTable table;
    public AbstractTasksPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;


        // Top: search bar + table
        final JBPanel<?> topPanel = new JBPanel<>(new BorderLayout());
        final JBPanel<?> jbPanel = buildBar();
        topPanel.add(jbPanel, BorderLayout.NORTH);

        taskTableModel
                .add(
                        new IconColumnRender<>("Type", t -> new IconData(t.getIcon()))
                        , new ColumnRenderer<>("Id", String.class, Task::getNumber, 20)
                        , new ColumnRenderer<>("Summary", String.class, Task::getSummary, 300)
                        , new ColumnRenderer<>("Updated", Date.class, Task::getUpdated, 120)
                        , new ColumnRenderer<>("Closed", Boolean.class, Task::isClosed, 10)
                );

        addCustomFields(taskTableModel);
        table = taskTableModel.createTable();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    Optional<T> task = getTask(table);

                    task.ifPresent(t -> openTask(t));

//                    task.map(Task::getIssueUrl).ifPresent(url -> {
//                        if (StringUtil.isNotEmpty(url)) {
//                            BrowserUtil.browse(url);
//                        }
//                    });
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            getTask(table).ifPresent(detailPanel::showTask);
        });

        topPanel.add(new JBScrollPane(table), BorderLayout.CENTER);

        // Splitter: top = table, bottom = details
        final JBSplitter splitter = new JBSplitter(
                true,        // true = horizontal split (top/bottom), false = vertical (left/right)
                "Tasks.splitter", // key to persist the split ratio in user settings
                0.6f         // initial ratio — 60% table, 40% details
        );
        splitter.setFirstComponent(topPanel);
        splitter.setSecondComponent(detailPanel);

        add(splitter, BorderLayout.CENTER);

        loadTasks(""); // load on init
        startAutoRefresh();

    }

    private void openTask(T task) {
        // Find or create the LocalTask from the tracker task
        final OpenTaskDialog openTaskDialog = new OpenTaskDialog(project, task);
        openTaskDialog.show();
//        LocalTask localTask = taskManager.findTask(task.getId());
//
//        if (localTask == null) {
//            // Activate it — this creates the LocalTask and shows the Open Task dialog
//            taskManager.activateTask(task, true);
//        } else {
//            // Already exists locally — show the dialog directly
//            final OpenTaskDialog openTaskDialog = new OpenTaskDialog(project, localTask);
//            openTaskDialog.show();
//        }
    }
    private int loadPersistedInterval() {
        return PropertiesComponent.getInstance(project).getInt(REFRESH_INTERVAL_KEY, DEFAULT_REFRESH_INTERVAL);
    }

    private void persistInterval(int value) {
        PropertiesComponent.getInstance(project).setValue(REFRESH_INTERVAL_KEY, value, DEFAULT_REFRESH_INTERVAL);
    }

    private Optional<T> getTask(final JBTable table) {
        return taskTableModel.getSelected(table);
    }

    private JBPanel<?> buildBar() {
        final JBPanel<JBPanel> bar = new JBPanel<>(new BorderLayout(4, 0));

        // Search field (center, takes all available width)
        final JBTextField searchField = new JBTextField();
        searchField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                final String text = searchField.getText();
                taskTableModel.filter(text);
            }
        });

        // Interval spinner (right side)
        final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(loadPersistedInterval(), 0, 3600, 5);

        final JSpinner intervalSpinner = new JSpinner(spinnerModel);
        intervalSpinner.setToolTipText("Auto-refresh interval (seconds) , set 0 to stop");
        // Constrain the spinner width — otherwise it stretches
        intervalSpinner.setPreferredSize(new Dimension(70, intervalSpinner.getPreferredSize().height));

        // Label + spinner grouped together
        final JBPanel<JBPanel> intervalPanel = new JBPanel<>(new BorderLayout(4, 0));
        final JBLabel intervalLabel = new JBLabel("Refresh:");
        intervalLabel.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        intervalLabel.setFontColor(UIUtil.FontColor.BRIGHTER);
        intervalPanel.add(intervalLabel, BorderLayout.WEST);
        intervalPanel.add(intervalSpinner, BorderLayout.CENTER);

        // React when spinner value changes
        intervalSpinner.addChangeListener(e -> {
            final int newInterval = (Integer) spinnerModel.getValue();
            persistInterval(newInterval);
            restartAutoRefresh(newInterval);
        });

        bar.add(searchField, BorderLayout.CENTER);
        bar.add(intervalPanel, BorderLayout.EAST);
        return bar;
    }

    protected abstract void addCustomFields(BoundTableModel<T> taskTableModel);

    private void restartAutoRefresh(int newIntervalSec) {
        stopAutoRefresh();
        scheduler = AppExecutorUtil.createBoundedScheduledExecutorService("TasksRefresh", 1);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        if (newIntervalSec > 0) {
            LOG.info("Starting query");
            scheduledFuture = scheduler.scheduleWithFixedDelay(() -> loadTasks(lastQuery), newIntervalSec, newIntervalSec, TimeUnit.SECONDS);
        } else {
            LOG.info("Stopped query");
        }
    }

    private void stopAutoRefresh() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void startAutoRefresh() {
        restartAutoRefresh(loadPersistedInterval());
    }

    public void dispose() {
        stopAutoRefresh();
    }

    private void loadTasks(String query) {
        // Run off EDT to avoid blocking UI
        lastQuery = query; // remember for auto-refresh
        ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Backgroundable(project, "Loading tasks") {
                                              @Override
                                              public void run(@NotNull ProgressIndicator indicator) {
                                                  try {
                                                      final T[] tasks = getTasks(indicator, query);
                                                      ApplicationManager.getApplication().invokeLater(() -> taskTableModel.set(Arrays.asList(tasks),table));
                                                  } catch (Exception e) {
                                                      LOG.error("Failed to invoke query ", e);
                                                      ApplicationManager.getApplication().invokeLater(() -> onQueryError(e)
                                                              //Messages.showErrorDialog(project, e.getMessage(), "Tasks Error")
                                                      );
                                                  }
                                              }
                                          }
        );
    }

    protected abstract void onQueryError(Exception e);

    protected abstract T[] getTasks(ProgressIndicator indicator, String query) throws Exception;

}