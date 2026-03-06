package org.azdtasks.plugin;


import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.ui.IconManager;
import com.intellij.util.Consumer;
import icons.TasksCoreIcons;
import icons.TasksIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Factory for creating Azure DevOps task repositories
 */
public class AzDoRepositoryType extends BaseRepositoryType<AzDoRepository> {

    
    public static final String NAME="AzDo";
    @NotNull
    @Override
    public String getName() {
        return NAME;
    }
    private final Icon icons = IconManager.getInstance().getIcon("META-INF/azdo.svg",this.getClass().getClassLoader());

    @NotNull
    @Override
    public Icon getIcon() {
        return icons;
    }

    @NotNull
    @Override
    public TaskRepository createRepository() {
        return new AzDoRepository(this);
    }

    @Override
    public Class<AzDoRepository> getRepositoryClass() {
        return AzDoRepository.class;
    }

    @Override
    public @NotNull TaskRepositoryEditor createEditor(AzDoRepository repository, Project project, Consumer<? super AzDoRepository> changeListener) {
        return new AzDoRepositoryEditor(project, repository, changeListener);
    }
}
