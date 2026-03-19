package org.tasklist.plugin;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskRepositoryType;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.azdtasks.core.WorkItemException;
import org.azdtasks.plugin.AzDoRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractTasksToolWindowFactory<R extends TaskRepository, T extends Task> implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final AbstractTasksPanel<T> panel = createTasksPanel(project);
        final ContentFactory contentFactory = ContentFactory.getInstance();
        final Content content = contentFactory.createContent(panel, getDisplayName(), false);
        content.setDisposer(panel::dispose);
        toolWindow.getContentManager().addContent(content);
    }

    protected abstract @NotNull AbstractTasksPanel<T> createTasksPanel(@NotNull Project project);

    protected @NotNull String getDisplayName() {
        return "";
    }

    protected Optional<R> get(Project project) {
        final TaskManager taskManager = TaskManager.getManager(project);
        final Optional<R> repo = Arrays.stream(taskManager.getAllRepositories())
                .filter(this::isMyRepo).findFirst()
                .map(v -> (R) v);
        return repo;
    }

    protected boolean isMyRepo(TaskRepository repo){
        return repo.isConfigured();
    };
}
