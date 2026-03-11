package org.azdtasks.plugin.tasklist;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.tasks.TaskManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.azdtasks.plugin.AzDoRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public class AzureTasksToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project,@NotNull ToolWindow toolWindow) {
        final AzureTasksPanel panel = new AzureTasksPanel(project);
        final ContentFactory contentFactory = ContentFactory.getInstance();
        final Content content = contentFactory.createContent(panel, "", false);
        content.setDisposer(panel::dispose);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // Only show if an Azure repo is configured
        return get(project).isPresent();
    }

    public static Optional<AzDoRepository> get(Project project){
        final TaskManager taskManager = TaskManager.getManager(project);
        final Optional<AzDoRepository> azDoRepository = Arrays.stream(taskManager.getAllRepositories())
                .filter(v -> v instanceof AzDoRepository).findFirst()
                .map(v -> (AzDoRepository) v);
        return azDoRepository;
    }

}