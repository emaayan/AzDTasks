package org.azdtasks.plugin;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.tasks.impl.TaskUiUtil;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.Consumer;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Configuration UI for Azure DevOps repository
 */
public class AzDoRepositoryEditor extends BaseRepositoryEditor<AzDoRepository> {

    private static final Logger LOG = Logger.getInstance(AzDoRepositoryEditor.class);

    private JBTextField organizationUrlField;
    private ComboBoxUpdater projects;
    private ComboBoxUpdater teams;
    //    private ComboBoxUpdater workTypesForBug;
//    private ComboBoxUpdater workTypesForFeature;
    private ComboBoxUpdater timeTrackingFieldName;
    private IntegerField topField;
    private JBTextField where;

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
    }


    @Override
    public void apply() {
        myRepository.setOrganization(organizationUrlField.getText().trim());
        myRepository.setWhere(where.getText().trim());
        projects.update();
        teams.update();
//        workTypesForBug.update();
//        workTypesForFeature.update();
        timeTrackingFieldName.update();


        final int value = topField.getValue();
        if (value > 0) {
            myRepository.setTop(value);
        }

        myTestButton.setEnabled(myRepository.canBeAccessed());
        super.apply();
    }

    @Override
    protected void afterTestConnection(boolean connectionSuccessful) {
        super.afterTestConnection(connectionSuccessful);
        LOG.info("Connection tested " + connectionSuccessful);
        if (connectionSuccessful) {
            updateProjectNamesInCombo();
        }
    }

    private class ComboBoxUpdater extends TaskUiUtil.ComboBoxUpdater<String> {

        private final Supplier<String> selectedItemSupplier;
        private final Consumer<String> selectedItemConsumer;
        private final Callable<Map<String, String>> onListFetcher;

        ComboBoxUpdater(String title, Supplier<String> selectedItemSupplier, Consumer<String> selectedItemConsumer, Callable<Map<String, String>> onListFetched) {
            super(AzDoRepositoryEditor.this.myProject, "Getting " + title, new ComboBox<>(200));
            myComboBox.setRenderer(SimpleListCellRenderer.create("", String::toString));
            this.selectedItemSupplier = selectedItemSupplier;
            this.selectedItemConsumer = selectedItemConsumer;
            this.onListFetcher = onListFetched;
            installListener(myComboBox);
        }


        @Override
        protected @NotNull List<String> fetch(@NotNull ProgressIndicator indicator) throws Exception {
            final Map<String, String> projects = onListFetcher.call();
            return new ArrayList<>(projects.values());
        }


        public void update() {
            final Object selectedItem = getCombo().getSelectedItem();
            if (selectedItem != null) {
                selectedItemConsumer.consume(selectedItem.toString());
            }
        }

        public JComboBox<String> getCombo() {
            return myComboBox;
        }

        @Override
        public @Nullable String getSelectedItem() {
            return selectedItemSupplier.get();
        }

        @Override
        protected void handleError() {
            super.handleError();
            //       myComboBox.removeAllItems();
        }

    }

    private void updateProjectNamesInCombo() {
        if (myRepository.canBeAccessed()) {
            projects.queue();
            teams.queue();
            timeTrackingFieldName.queue();
        }
        myTestButton.setEnabled(myRepository.canBeAccessed());
    }

    @Nullable
    @Override
    protected JComponent createCustomPanel() {
        LOG.info("Building panel");

        organizationUrlField = new JBTextField(myRepository.getOrganization());
        organizationUrlField.setToolTipText("Azure DevOps organization");
        organizationUrlField.getEmptyText().setText("Organization");
        installListener(organizationUrlField);

        topField = new IntegerField("Max query results", 1, 200);
        topField.setCanBeEmpty(false);
        topField.setDefaultValue(myRepository.getTop());
        topField.setValue(myRepository.getTop());
        topField.setPreferredSize(new Dimension(50, topField.getPreferredSize().height));
        topField.setMaximumSize(topField.getPreferredSize());
        topField.setToolTipText("Maximum amount of items a open tasks query will return");
        final JBPanel topFieldPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));//prevent field to strech
        topFieldPanel.add(topField);
        installListener(topField);

        projects = new ComboBoxUpdater("Projects", myRepository::getProject, myRepository::setProject, () -> myRepository.getProjects().get());
        teams = new ComboBoxUpdater("Teams", myRepository::getTeam, myRepository::setTeam, () -> myRepository.getTeams().get());
        final Callable<Map<String, String>> mapCallable = () -> {
            final Object selectedItem = projects.getCombo().getSelectedItem();
            if (selectedItem != null) {
                final String project = selectedItem.toString();
                final Map<String, String> workItemTypes = myRepository.getWorkItemTypes(project);
                return workItemTypes;
            } else {
                return Map.of();
            }
        };

//        workTypesForBug = new ComboBoxUpdater("Work Items for Bug", myRepository::getBugWorkItemType, myRepository::setBugWorkItemType, mapCallable);
//        workTypesForFeature = new ComboBoxUpdater("Work Items for Feature", myRepository::getFeatureWorkItemType, myRepository::setFeatureWorkItemType, mapCallable);
        projects.getCombo().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                try {
//                    workTypesForBug.queue();
//                    workTypesForFeature.queue();
                } catch (Throwable t) {
                    LOG.error("Error", t);
                }
            }
        });

        timeTrackingFieldName = new ComboBoxUpdater("Fields for time tracking", myRepository::getTimeTrackFieldName, myRepository::setTimeTrackFieldName, myRepository::getWorkItemFieldsForTimeTrack);
        updateProjectNamesInCombo();

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

        final JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Organization:", organizationUrlField)
                .addComponent(myTestButton)
                .addComponent(helpLabel)
                .addLabeledComponent("Project:", projects.getCombo())
                .addLabeledComponent("Team:", teams.getCombo())
                .addLabeledComponent("Max items for query:", topFieldPanel)
//                .addLabeledComponent("Bug work item type:", workTypesForBug.getCombo())
//                .addLabeledComponent("Feature work item type:", workTypesForFeature.getCombo())
                .addLabeledComponent("Field for time tracking:", timeTrackingFieldName.getCombo())
                .addLabeledComponent("Where clause:", where)
//                .addComponentFillVertically(new JBPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)), 0)
                .getPanel();

        return new JBScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


    }

}
