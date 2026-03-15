package org.azdtasks.plugin.tasklist;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.TaskRepository;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import kotlin.coroutines.Continuation;
import org.azdtasks.core.WorkItemException;
import org.azdtasks.plugin.AzDoRepository;
import org.azdtasks.plugin.AzTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tasklist.plugin.AbstractTasksPanel;
import org.tasklist.plugin.AbstractTasksToolWindowFactory;
import org.tasklist.plugin.BoundTableModel;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class AzureTasksToolWindowFactory extends AbstractTasksToolWindowFactory<AzDoRepository, AzTask> {

    @Override
    protected @NotNull AbstractTasksPanel<AzTask> createTasksPanel(@NotNull Project project) {
        return new AbstractTasksPanel<>(project) {
            @Override
            protected AzTask[] getTasks(ProgressIndicator indicator, String query) throws Exception {
                Optional<AzDoRepository> azDoRepository1 = get(project);
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
                        taskTableModel.new Column<String>("State", String.class, t -> t.getWorkItemModel().state(), 10)
                );
            }
        };
    }

    // Only show if an Azure repo is configured
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return get(project).isPresent();
    }

    @Override
    protected boolean isMyRepo(TaskRepository repo) {
        return repo.getRepositoryType().getRepositoryClass().equals(AzDoRepository.class);
    }

}