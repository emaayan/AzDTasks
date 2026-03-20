package org.tasklist.plugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.TaskRepository;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * An abstract base class for creating tool window factories for handling task management within an IDE.
 * This class provides the functionality to initialize and display a tool window with a custom tasks panel.
 * this is meant to be reused even by other Issue Tracker plugin as a seperate plugin
 * @param <R> the type of the task repository associated with the tasks tool window
 * @param <T> the type of the task associated with the tasks tool window
 */
public abstract class AbstractTasksToolWindowFactory<R extends TaskQueryProvider<T>, T extends Task> implements ToolWindowFactory {

    private final static Logger LOG = Logger.getInstance(AbstractTasksToolWindowFactory.class);
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final AbstractTasksPanel<T> panel = createTasksPanel(project);
        final ContentFactory contentFactory = ContentFactory.getInstance();
        final Content content = contentFactory.createContent(panel, getDisplayName(), false);
        content.setDisposer(panel::dispose);
        toolWindow.getContentManager().addContent(content);
    }


    protected @NotNull String getDisplayName() {
        return "";
    }

    protected Optional<R> get(Project project) {
        if(LOG.isDebugEnabled()){
            LOG.debug("Checking for repo for "+project);
        }
        final TaskManager taskManager = TaskManager.getManager(project);
        final TaskRepository[] allRepositories = taskManager.getAllRepositories();
        final Class<R> providerClass = getProviderClass();
        final Optional<R> first = Arrays.stream(allRepositories)
                .filter( t -> t.isConfigured() && providerClass.isAssignableFrom(t.getClass()))
                .map(providerClass::cast)
                .filter(this::isMyRepo).findFirst();
        return first;
    }

    protected boolean isMyRepo(R repo){
        return repo.isReady();
    }

    protected abstract @NotNull AbstractTasksPanel<T> createTasksPanel(@NotNull Project project);

    protected abstract Class<R> getProviderClass();

}
