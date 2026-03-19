package org.azdtasks.plugin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.AtomicClearableLazyValue;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.tasks.Comment;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskType;
import com.intellij.ui.IconManager;
import com.intellij.util.IconUtil;
import org.azdtasks.core.WorkItemComments;
import org.azdtasks.core.WorkItemModel;
import org.azdtasks.core.WorkItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class AzTask extends Task {
    private final AzDoRepository azDoRepository;
    private final WorkItemModel workItemModel;
    private final TaskType taskType;
    private final boolean isClosed;
    private final String iconUrl;

    public AzTask(AzDoRepository azDoRepository, TaskType taskType, boolean isClosed, WorkItemModel workItemModel, String iconURL) {
        this.azDoRepository = azDoRepository;
        this.workItemModel = workItemModel;
        this.taskType = taskType;
        this.isClosed = isClosed;
        this.iconUrl = iconURL;
    }


    @NotNull
    @Override
    public String getId() {
        final String s = azDoRepository.buildId(getNumber());
        return s;
    }

    @Override
    public @NotNull String getNumber() {//used for updating time and tasks stats
        return String.valueOf(workItemModel.id());
    }

    @Override
    public @NlsSafe @NotNull String getPresentableId() {//used for change list names, and commit message
        return "%s %s".formatted(workItemModel.workItemType(), getNumber());
    }


    @NotNull
    @Override
    public String getPresentableName() {//use for the search box
        return "%s %s: %s".formatted(getProject(), getPresentableId(), getSummary());
    }


    @NotNull
    @Override
    public String getSummary() {
        return workItemModel.title();
    }

    @Nullable
    @Override
    public String getDescription() {
        return workItemModel.description();
    }


    @Override
    public Comment @NotNull [] getComments() {
        final WorkItemComments workItemComments = workItemModel.workItemComments();
        final Comment[] comments;
        if (workItemComments != null && workItemComments.comments() != null) {
            final List<WorkItemComments.WorkItemComment> comments1 = workItemComments.comments();
            comments = new Comment[comments1.size()];
            for (int i = 0; i < comments1.size(); i++) {
                final WorkItemComments.WorkItemComment workItemComment = comments1.get(i);
                comments[i] = new Comment() {
                    @Override
                    public @NlsSafe String getText() {
                        return workItemComment.text();
                    }

                    @Override
                    public @Nullable @NlsSafe String getAuthor() {
                        return workItemComment.author();
                    }

                    @Override
                    public @Nullable Date getDate() {
                        return workItemComment.createdDate();
                    }
                };
            }
        } else {
            comments = Comment.EMPTY_ARRAY;
        }
        return comments;
    }


//    @Override
//    public @Nullable String getCustomIcon() {//don't return the customURL icon without resizing it first
//        return iconUrl;// super.getCustomIcon();
//    }

    private final EnumMap<TaskType, Icon> icons = new EnumMap<>(Map.of(
            TaskType.BUG, IconManager.getInstance().getIcon("META-INF/bug.svg", this.getClass().getClassLoader())
            , TaskType.EXCEPTION, IconManager.getInstance().getIcon("META-INF/exception.svg", this.getClass().getClassLoader())
            , TaskType.FEATURE, IconManager.getInstance().getIcon("META-INF/feature.svg", this.getClass().getClassLoader())
            , TaskType.OTHER, IconManager.getInstance().getIcon("META-INF/misc.svg", this.getClass().getClassLoader())
    ));

    @Override
    public @NotNull Icon getIcon() {
        final String customIcon = iconUrl;// getCustomIcon();
        final Icon icon1 = azDoRepository.fetchIcon(customIcon).orElseGet(() -> {
            final TaskType type = getType();
            final Icon icon = icons.getOrDefault(type, AllIcons.FileTypes.Any_type);
            return icon;
        }
        );
        return icon1;
    }


    @Override
    public @NotNull TaskType getType() {
        return taskType;
    }

    @Nullable
    @Override
    public String getIssueUrl() {
        return workItemModel.url();
    }

    @Nullable
    @Override
    public String getProject() {
        return azDoRepository.getProject();
    }

    @Nullable
    @Override
    public TaskRepository getRepository() {
        return azDoRepository;
    }


    @Override
    public boolean isClosed() {
        return this.isClosed;
    }

    @Override
    public boolean isIssue() {
        return true;
    }

    @Nullable
    @Override
    public Date getUpdated() {
        return workItemModel.changedDate();
    }

    @Nullable
    @Override
    public Date getCreated() {
        return workItemModel.createdDate();
    }

    public WorkItemModel getWorkItemModel() {
        return workItemModel;
    }
}
