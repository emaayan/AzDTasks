package org.tasklist.plugin;

import com.intellij.tasks.Task;
import org.azdtasks.core.WorkItemException;
import org.azdtasks.plugin.AzTask;
import org.jetbrains.annotations.NotNull;

public interface TaskQueryProvider<T extends Task>  {

    T @NotNull [] getCurrentTasks() throws Exception;
    default boolean isReady(){
        return true;
    }
}
