package org.tasklist.plugin;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.Comment;
import com.intellij.tasks.Task;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.tasklist.plugin.table.BoundTableModel;
import org.tasklist.plugin.table.ColumnRenderer;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TaskDetailPanel<T extends Task> extends JBPanel<TaskDetailPanel<? extends Task>> {

    private final JBLabel idLabel = new JBLabel();
    private final JBLabel summaryLabel = new JBLabel();
    private final JBLabel statusLabel = new JBLabel();
    private final JBLabel updatedLabel = new JBLabel();

    private final JEditorPane descPane = new JEditorPane(UIUtil.HTML_MIME, "");
    private final JButton openButton = new JButton("Open in Browser");

    private final BoundTableModel<Comment> taskCommentTableModel = new BoundTableModel<>();
    private final JBTable table;

    private T task = null;


    public TaskDetailPanel() {
        super(new BorderLayout(0, 8));

        setBorder(JBUI.Borders.empty(8));
        descPane.setEditable(false);
        descPane.setBackground(UIUtil.getPanelBackground());
        descPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                BrowserUtil.browse(e.getURL());
        });

        final JBPanel<JBPanel> descPanel = new JBPanel<>(new BorderLayout(0, 4));
        descPanel.setBorder(JBUI.Borders.emptyBottom(4));
        descPanel.add(buildMetaPanel(), BorderLayout.NORTH);
        descPanel.add(new JBScrollPane(descPane), BorderLayout.CENTER);
        descPanel.add(openButton, BorderLayout.SOUTH);


        // Bottom half: comments
        final JBPanel<JBPanel> commentsPanel = new JBPanel<>(new BorderLayout());
        final JBLabel commentsTitle = new JBLabel("Comments");
        commentsTitle.setBorder(JBUI.Borders.emptyBottom(4));
        commentsTitle.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        commentsTitle.setFontColor(UIUtil.FontColor.BRIGHTER);
        commentsPanel.add(commentsTitle, BorderLayout.NORTH);

        buildTable(taskCommentTableModel);
        table = taskCommentTableModel.createTable();
        commentsPanel.add(new JBScrollPane(table), BorderLayout.CENTER);

        // Inner splitter: description (top) / comments (bottom)
        final JBSplitter detailSplitter = new JBSplitter(
                true,
                "AzureTasks.detailSplitter",
                0.5f
        );
        detailSplitter.setFirstComponent(descPanel);
        detailSplitter.setSecondComponent(commentsPanel);

        add(detailSplitter, BorderLayout.CENTER);
        openButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (task != null) {
                    openButton.addActionListener(l -> BrowserUtil.browse(Objects.requireNonNull(task.getIssueUrl())));
                }
            }
        });
        showEmpty(); // blank state until a row is selected
    }

    private JBPanel<JBPanel> buildMetaPanel() {
        // 2-column grid: label | value
        final JBPanel<JBPanel> grid = new JBPanel<>(new GridBagLayout());
        grid.setBackground(UIUtil.getPanelBackground());
        final GridBagConstraints labelC = new GridBagConstraints();
        labelC.anchor = GridBagConstraints.WEST;
        labelC.insets = JBUI.insets(2, 0, 2, 8);

        final GridBagConstraints valueC = new GridBagConstraints();
        valueC.anchor = GridBagConstraints.WEST;
        valueC.weightx = 1.0;
        valueC.fill = GridBagConstraints.HORIZONTAL;
        valueC.gridwidth = GridBagConstraints.REMAINDER;

//        addRow(grid, labelC, valueC, "ID:", idLabel);
        addField(grid, labelC, valueC, "Summary:", summaryLabel);
        //addRow(grid, labelC, valueC, "Status:", statusLabel);
        addField(grid, labelC, valueC, "Updated:", updatedLabel);
        addCustomFields(grid, labelC, valueC);
        return grid;
    }

    protected void addField(JPanel grid, GridBagConstraints lc, GridBagConstraints vc, String labelText, JBLabel valueLabel) {
        final JBLabel label = new JBLabel(labelText);
        label.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        label.setFontColor(UIUtil.FontColor.BRIGHTER); // dimmed label
        valueLabel.setComponentStyle(UIUtil.ComponentStyle.REGULAR);
        grid.add(label, lc);
        grid.add(valueLabel, vc);
    }

    protected void showCustomTask(T task) {

    }

    protected void showTask(T task) {
        this.task = task;
        if (task != null) {
            idLabel.setText(task.getPresentableId());
            //typeLabel.setText(task.getType().name());
            summaryLabel.setText(task.getSummary());
            //statusLabel.setText(task.getState() != null ? task.getState().name() : "—");
            updatedLabel.setText(task.getUpdated() != null ? task.getUpdated().toString() : "—");

            final String description = task.getDescription();
            descPane.setText(StringUtil.isNotEmpty(description)
                    ? "<html><body style='font-family:sans-serif'>%s</body></html>".formatted(description)
                    : "<html><body style='color:gray;font-style:italic'>No Description.</body></html>");
            descPane.setCaretPosition(0); // scroll to top

            openButton.setVisible(task.getIssueUrl() != null);

            final Comment[] comments = task.getComments();
            if (comments.length > 0) {
                //commentsModel.replaceAll(Arrays.asList(comments));
                taskCommentTableModel.set(List.of(comments),table);
            } else {
                taskCommentTableModel.set(List.of(),table);
                //commentsModel.removeAll();
            }
            showCustomTask(task);
        } else {
            showEmpty();
        }
    }

    private void showEmpty() {
        descPane.setText(getEmptyText());
        openButton.setVisible(false);
        taskCommentTableModel.set(List.of(),table);
//        commentsModel.removeAll();
    }

    protected void buildTable(BoundTableModel<Comment> taskCommentTableModel){
        taskCommentTableModel.add(
                new ColumnRenderer<>("Author", String.class, Comment::getAuthor, 10)
                , new ColumnRenderer<>("Updated", Date.class, Comment::getDate, 50)
                , new ColumnRenderer<>("Comment", String.class, Comment::getText, 200)
        );
    }
    protected void addCustomFields(JPanel grid, GridBagConstraints lc, GridBagConstraints vc) {

    }

    protected @NotNull String getEmptyText() {
        return """
                <html>
                <body style='color:gray;font-style:italic;padding:8px'>
                    Select a task to see details.
                </body>
                </html>""";
    }
}
