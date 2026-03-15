package org.tasklist.plugin;


import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiManager;
import com.intellij.tasks.Task;
import com.intellij.tasks.doc.TaskPsiElement;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import s.D.F;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTasksPanel<T extends Task> extends JBPanel<AbstractTasksPanel<? extends Task>> {

    private static final String REFRESH_INTERVAL_KEY = "AzureTasks.refreshInterval";
    private static final int DEFAULT_REFRESH_INTERVAL = 30;

    private final Project project;
    private final BoundTableModel<T> taskTableModel = new BoundTableModel<>();
    private final TaskDetailPanel<T> detailPanel = new TaskDetailPanel<>();
    private String lastQuery = "";
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture = null;


    public AbstractTasksPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;

        //support Ctrl+Q directly on the table
//        final String showQuickDoc = "showQuickDoc";
//        table.getActionMap().put(showQuickDoc, new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                getTask(table).ifPresent(task -> showDocumentation(task));
//            }
//        });
//        table.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), showQuickDoc);


        // Top: search bar + table
        final JBPanel<JBPanel> topPanel = new JBPanel<>(new BorderLayout());
        final JBPanel jbPanel = buildSearchBar();
        topPanel.add(jbPanel, BorderLayout.NORTH);


        taskTableModel
                .add(
                        taskTableModel.new Column<String>("Id", String.class, Task::getPresentableId, 20)
                        , taskTableModel.new Column<String>("Summary", String.class, Task::getSummary, 300)
                        , taskTableModel.new Column<Date>("Updated", Date.class, Task::getUpdated, 120)
                        , taskTableModel.new Column<Boolean>("Closed", Boolean.class, Task::isClosed, 10)
                );
        addCustomFields(taskTableModel);
        final JBTable table = taskTableModel.createTable();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    getTask(table).map(Task::getIssueUrl).ifPresent(url -> {
                        if (StringUtil.isNotEmpty(url)) {
                            BrowserUtil.browse(url);
                        }
                    });
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

    private int loadPersistedInterval() {
        return PropertiesComponent.getInstance(project).getInt(REFRESH_INTERVAL_KEY, DEFAULT_REFRESH_INTERVAL);
    }

    private void persistInterval(int value) {
        PropertiesComponent.getInstance(project).setValue(REFRESH_INTERVAL_KEY, value, DEFAULT_REFRESH_INTERVAL);
    }

    private Optional<T> getTask(final JBTable table) {
        return taskTableModel.getSelected(table);
    }

    private JBPanel buildSearchBar() {
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
        final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(loadPersistedInterval(), 5, 3600, 5);
        final JSpinner intervalSpinner = new JSpinner(spinnerModel);
        intervalSpinner.setToolTipText("Auto-refresh interval (seconds)");

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

    protected void addCustomFields(BoundTableModel<T> taskTableModel) {

    }

//    private void showDocumentation(T task) {
//        final PsiManager instance = PsiManager.getInstance(project);
//        final TaskPsiElement element = new TaskPsiElement(instance, task);
//        // Reuses IntelliJ's exact documentation UI
//        com.intellij.codeInsight.documentation.DocumentationManager.getInstance(project).showJavaDocInfo(element, null);
//    }

    private void restartAutoRefresh(int newIntervalSec) {
        stopAutoRefresh();
        scheduler = AppExecutorUtil.createBoundedScheduledExecutorService("TasksRefresh", 1);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = scheduler.scheduleWithFixedDelay(() -> loadTasks(lastQuery), newIntervalSec, newIntervalSec, TimeUnit.SECONDS);
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
        //if (countdownTimer != null) countdownTimer.stop();
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
                                                  try {
                                                      final T[] tasks = getTasks(indicator, query);
                                                      ApplicationManager.getApplication().invokeLater(() -> taskTableModel.set(Arrays.asList(tasks)));
                                                  } catch (Exception e) {
                                                      ApplicationManager.getApplication().invokeLater(() ->
                                                              Messages.showErrorDialog(project, e.getMessage(), "Tasks Error")
                                                      );
                                                  }
                                              }
                                          }
        );
    }

    protected abstract T[] getTasks(ProgressIndicator indicator, String query) throws Exception;

}