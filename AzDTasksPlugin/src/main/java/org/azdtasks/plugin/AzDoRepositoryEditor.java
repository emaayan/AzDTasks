package org.azdtasks.plugin;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.tasks.Task;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.tasks.impl.TaskUiUtil;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.*;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.Consumer;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.azdtasks.core.WorkItemException;
import org.azdtasks.core.client.WorkItemClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Configuration UI for Azure DevOps repository
 */
public class AzDoRepositoryEditor extends BaseRepositoryEditor<AzDoRepository> {

    private static final Logger LOG = Logger.getInstance(AzDoRepositoryEditor.class);

    private JBTextField organization;

    private ComboBoxUpdater projects;
    private ComboBoxUpdater teams;
    private ComboBoxUpdater timeTrackingFieldName;

    private IntegerField topField;
    private JBTextField where;
    private JButton testQuery;

    private AtomicInteger comboCounter = new AtomicInteger();

    public AzDoRepositoryEditor(Project project, AzDoRepository repository, Consumer<? super AzDoRepository> changeListener) {
        super(project, repository, changeListener);
        LOG.info("Started Editor");
        myUsernameLabel.setVisible(false);
        myUserNameText.setVisible(false);
        myPasswordLabel.setVisible(true);
        myPasswordLabel.setText("Token:");
        myPasswordText.setVisible(true);
        myPasswordText.setToolTipText("Personal Access Token with Work Items read permission");
        myUrlLabel.setVisible(true);
        myURLText.setVisible(true);
        myURLText.setEnabled(false);
        myTestButton.setEnabled(false);
        myShareUrlCheckBox.setVisible(false);
        UIUtil.invokeLaterIfNeeded(this::updateProjectNamesInCombo);
    }


    @Override
    public void apply() {
        myRepository.setOrganization(organization.getText().trim());

        projects.update();
        teams.update();
        timeTrackingFieldName.update();

        final int value = topField.getValue();
        if (value > 0) {
            myRepository.setTop(value);
        }

        myTestButton.setEnabled(myRepository.canBeAccessed());
        myRepository.setWhere(where.getText().trim());
        super.apply();
    }

    @Override
    protected void afterTestConnection(boolean connectionSuccessful) {
        super.afterTestConnection(connectionSuccessful);
        LOG.info("Connection tested " + connectionSuccessful);
        updateProjectNamesInCombo();
    }

    private class ComboBoxUpdater extends TaskUiUtil.ComboBoxUpdater<String> {

        private final Supplier<String> selectedItemSupplier;
        private final Consumer<String> selectedItemConsumer;
        private final Callable<Map<String, String>> onListFetcher;
        private final Consumer<ComboBoxUpdater> onFinish;

        ComboBoxUpdater(String title, Supplier<String> selectedItemSupplier, Consumer<String> selectedItemConsumer, Callable<Map<String, String>> onListFetched, Consumer<ComboBoxUpdater> onFinish) {
            super(AzDoRepositoryEditor.this.myProject, "Getting " + title, new ComboBox<>(200));
            myComboBox.setRenderer(SimpleListCellRenderer.create("", String::toString));
            installListener(myComboBox);
            this.selectedItemSupplier = selectedItemSupplier;
            this.selectedItemConsumer = selectedItemConsumer;
            this.onListFetcher = onListFetched;
            this.onFinish = onFinish;
        }

        @Override
        protected @NotNull List<String> fetch(@NotNull ProgressIndicator indicator) throws Exception {
            this.myResult = null;// to clear the combo in case of an error
            final Map<String, String> projects = onListFetcher.call();
            return new ArrayList<>(projects.values());
        }

        @Override
        public void onFinished() {
            super.onFinished();
            this.onFinish.accept(this);
        }

        @Override
        protected void handleError() {
            myComboBox.removeAllItems();
        }

        @Override
        public @Nullable String getSelectedItem() {
            return selectedItemSupplier.get();
        }

        public JComboBox<String> getCombo() {
            return myComboBox;
        }

        public void update() {
            final Object selectedItem = getCombo().getSelectedItem();
            if (selectedItem != null) {
                selectedItemConsumer.consume(selectedItem.toString());
            }
        }

    }

    private void comboFinishedLoading() {
        LOG.info("ComboFinished " + comboCounter.get());
        int i = comboCounter.decrementAndGet();
        myCustomPanel.setEnabled(i==0);
    }


    private WorkItemClient client;
    private void updateProjectNamesInCombo() {
        if (myRepository.canBeAccessed()) {
            try {
                this.myCustomPanel.setEnabled(false);
                this.comboCounter.set(3);
                this.client=myRepository.createClient();
                projects.queue();
                teams.queue();
                timeTrackingFieldName.queue();
            } catch (WorkItemException e) {
                LOG.error("Error invoking client",e);
            }
        }
    }

    @Nullable
    @Override
    protected JComponent createCustomPanel() {
        LOG.info("Building panel");
        organization = new JBTextField(myRepository.getOrganization());
        organization.setToolTipText("Azure DevOps organization");
        organization.getEmptyText().setText("Organization");
        installListener(organization);

        topField = new IntegerField("Max query results", 1, 200);
        topField.setCanBeEmpty(false);
        topField.setDefaultValue(myRepository.getTop());
        topField.setValue(myRepository.getTop());
        topField.setPreferredSize(new Dimension(50, topField.getPreferredSize().height));
        topField.setMaximumSize(topField.getPreferredSize());
        topField.setToolTipText("Maximum amount of items a open tasks query will return");
        final JBPanel<JBPanel> topFieldPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));//prevent field to strech
        topFieldPanel.add(topField);
        installListener(topField);

        final Consumer<ComboBoxUpdater> comboBoxUpdaterConsumer = comboBoxUpdater -> comboFinishedLoading();
        projects = new ComboBoxUpdater("Projects", myRepository::getProject, myRepository::setProject, () -> {
            return client.getProjects();
        }, comboBoxUpdaterConsumer);
        teams = new ComboBoxUpdater("Teams", myRepository::getTeam, myRepository::setTeam, new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                return client.getTeams();
            }
        }, comboBoxUpdaterConsumer);
        timeTrackingFieldName = new ComboBoxUpdater("Fields for time tracking", myRepository::getTimeTrackFieldName, myRepository::setTimeTrackFieldName, new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                return AzDoRepository.getWorkItemFieldsForTimeTrack(client);
            }
        }, comboBoxUpdaterConsumer);


        // Help text
        final JBLabel helpLabel = new JBLabel("""
                <html><body style='width: 400px'>\
                <b>Setup Instructions:</b><br>\
                1. Enter your organization name<br>\
                2. Go to Azure DevOps → User Settings → Personal Access Tokens<br>\
                3. Create a new token with 'Work Items (Read,write)' scope<br>\
                4. Copy the token and paste it in the Personal Access Token field<br>\
                5. Click 'Test Connection' to verify and fill in the combo boxes<br>\
                </body></html>"""
        );
        helpLabel.setForeground(UIUtil.getContextHelpForeground());
        helpLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        final String where1 = myRepository.getWhere();
        where = new JBTextField(where1);
//        where.setPreferredSize(new Dimension(100, topField.getPreferredSize().height));
//        where.setMaximumSize(topField.getPreferredSize());
        where.setToolTipText("Custom where clause for the task view");
        where.getEmptyText().setText(AzDoRepository.DEFAULT_WHERE);
        installListener(where);

        testQuery = new JButton("Test query");

        testQuery.addActionListener(e -> {
            try {
                final Task[] ts = ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.WithResult<Task[], Exception>(myProject, "Load Tasks", true) {
                    @Override
                    protected Task[] compute(@NotNull ProgressIndicator indicator) throws Exception {
                        indicator.checkCanceled();
                        return myRepository.getCurrentTasks();
                    }
                });
                Messages.showInfoMessage("Number of tasks " + ts.length, "Results");
            } catch (ProcessCanceledException exx) {
                throw exx;
            } catch (Exception ex) {
                LOG.error("Failed to invoke query", ex);
                Messages.showErrorDialog("Error executing query " + ex.getMessage(), "Results");
            }

        });

        //                .addComponentFillVertically(new JBPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)), 0)
        final JPanel formPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Organization:", organization)
                .addComponent(myTestButton)
                .addComponent(helpLabel)
                .addLabeledComponent("Project:", projects.getCombo())
                .addLabeledComponent("Team:", teams.getCombo())
                .addLabeledComponent("Max items for query:", topFieldPanel)
                .addLabeledComponent("Field for time tracking:", timeTrackingFieldName.getCombo())
                .addLabeledComponent("Where clause:", where)
                .addComponent(testQuery)
//                .addComponentFillVertically(new JBPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)), 0)
                .getPanel();
        comboCounter = new AtomicInteger();
        return new JBScrollPane(formPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    }
}
