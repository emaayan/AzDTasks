package org.azdtasks.plugin.tasklist;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import org.azdtasks.core.WorkItemException;
import org.azdtasks.plugin.AzDoRepository;
import org.azdtasks.plugin.AzTask;
import org.jetbrains.annotations.NotNull;
import org.tasklist.plugin.AbstractTasksPanel;
import org.tasklist.plugin.AbstractTasksToolWindowFactory;
import org.tasklist.plugin.TaskQueryProvider;
import org.tasklist.plugin.table.BoundTableModel;
import org.tasklist.plugin.table.ColumnRenderer;

import java.util.Optional;

public class AzureTasksToolWindowFactory extends AbstractTasksToolWindowFactory<AzDoRepository, AzTask> {


    @Override
    protected @NotNull AbstractTasksPanel<AzTask> createTasksPanel(@NotNull Project project) {
        return new AbstractTasksPanel<>(project) {
            @Override
            protected AzTask[] getTasks(ProgressIndicator indicator, String query) throws Exception {
                Optional<AzDoRepository> azDoRepository1 = get(project);
                indicator.checkCanceled();
                final AzTask[] tasks = azDoRepository1.map(azDoRepository -> {
                    try {
                        return azDoRepository.getCurrentTasks();
                    } catch (WorkItemException e) {
                        throw new RuntimeException(e);
                    }
                }).orElse(new AzTask[]{});
                return tasks;
            }

            @Override
            protected void addCustomFields(BoundTableModel<AzTask> taskTableModel) {
                taskTableModel.add(
                        new ColumnRenderer<>("State", String.class, t -> t.getWorkItemModel().state(), 10)
                );
            }

            @Override
            protected void onQueryError(Exception e) {
                final String message = e.getMessage();
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("Azure Tasks")
                        .createNotification("AzureTasks", message, NotificationType.ERROR)
                        .notify(project);
            }
        };
    }

    @Override
    protected Class<AzDoRepository> getProviderClass() {
        return AzDoRepository.class;
    }


//    @Override
//    protected boolean isMyRepo(TaskQueryProvider<AzTask> repo) {
//        return super.isMyRepo(repo) && repo.getRepositoryType().getRepositoryClass().equals(AzDoRepository.class);
//    }

}