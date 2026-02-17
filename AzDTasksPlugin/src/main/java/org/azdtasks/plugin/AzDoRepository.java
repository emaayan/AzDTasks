package org.azdtasks.plugin;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.httpclient.NewBaseRepositoryImpl;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import icons.TasksIcons;
import org.azd.exceptions.AzDException;

import org.azdtasks.core.WorkItemException;
import org.azdtasks.core.WorkItemType;
import org.azdtasks.core.client.AbstractWorkItemClient;
import org.azdtasks.core.client.WorkItemClient;
import org.azdtasks.core.WorkItemComments;
import org.azdtasks.core.WorkItemModel;
import org.azdtasks.core.client.WorkItemLegacyClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Azure DevOps task repository implementation
 */
@Tag(AzDoRepositoryType.NAME)
public class AzDoRepository extends NewBaseRepositoryImpl {

    private static final Logger LOG = Logger.getInstance(AzDoRepository.class);

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private transient AbstractWorkItemClient client;
    
    private final Map<TaskType, String> taskTypeToWorkItemTypeMap = new HashMap<>();
    private final Map<String, TaskType> workItemTypeToTaskTypeMap = new HashMap<>();

    //required for reflection
    public AzDoRepository() {
        super();
        setBugWorkItemType("Bug");
        setFeatureWorkItemType("Feature");
    }

    public AzDoRepository(TaskRepositoryType type) {
        super(type);
        setBugWorkItemType("Bug");
        setFeatureWorkItemType("Feature");
    }

    private AzDoRepository(AzDoRepository other) {
        super(other);
        LOG.info("Cloning AzDoRepository");
        setOrganization(other.getOrganization());
        setProject(other.getProject());
        setTeam(other.getTeam());
        setTop(other.getTop());
        workItemTypeToTaskTypeMap.putAll(other.workItemTypeToTaskTypeMap);
        taskTypeToWorkItemTypeMap.putAll(other.taskTypeToWorkItemTypeMap);
    }

    //used non standard naming conventions to avoid xml serializer
    public AbstractWorkItemClient useClient() {
        return client;
    }
    //used non standard naming conventions to avoid xml serializer
    public void saveClient(AbstractWorkItemClient client) {
        this.client = client;
    }

    @NotNull
    @Override
    public BaseRepository clone() {
        return new AzDoRepository(this);
    }
    
    public CompletableFuture<Map<String, String>> getProjects() {
        final String password = getPassword();
        if (canBeAccessed()) {
            final AbstractWorkItemClient abstractWorkItemClient = new WorkItemLegacyClient(organization, password);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return abstractWorkItemClient.getProjects();
                } catch (WorkItemException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    public final CompletableFuture<Map<String, String>> getTeams() {
        final String password = getPassword();
        if (canBeAccessed()) {
            final AbstractWorkItemClient abstractWorkItemClient = new WorkItemLegacyClient(organization, password);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return abstractWorkItemClient.getTeams();
                } catch (WorkItemException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    public Map<String, String> getWorkItemTypes() throws AzDException {
        final String project1 = getProject();
        return getWorkItemTypes(project1);
    }

    public @NotNull Map<String, String> getWorkItemTypes(String project1) throws AzDException {
        final String organizationUrl1 = getOrganization();
        final String password = getPassword();
        if (canBeAccessed() && !isEmpty(project1)) {
            final AbstractWorkItemClient client = new WorkItemLegacyClient(organizationUrl1, project1, password);
            final Map<String, WorkItemType> workItemTypes1 =client.getWorkItemTypes();
            final Map<String, String> m = workItemTypes1.keySet()
                    .stream()
                    .collect(Collectors.toMap(s -> s, s -> s));
            return m;    
        }else{
            LOG.error("Not Configured");
            return Map.of();
        }
    }

    @Override
    public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed, @NotNull ProgressIndicator cancelled) throws Exception {
        ensureClient();
        try {
            final List<WorkItemModel> workItems = isEmpty(query) ? List.of() : useClient().searchWorkItems(query, getTeam());
            return workItems.parallelStream()
                    .map(this::convertToTask)
                    .toArray(Task[]::new);

        } catch (AzDException e) {
            LOG.error("Failed to fetch work items", e);
            throw new Exception("Failed to fetch work items: " + e.getMessage(), e);
        }
    }


    @Nullable
    @Override
    public Task findTask(@NotNull String id) throws Exception {
        ensureClient();
        try {
            final int workItemId = Integer.parseInt(id);
            final WorkItemModel workItem = useClient().getWorkItem(workItemId);
            if (workItem == null) {
                return null;
            }
            return convertToTask(workItem);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid work item ID: " + id);
            return null;
        } catch (AzDException e) {
            LOG.error("Failed to find work item " + id, e);
            throw new Exception("Failed to find work item: " + e.getMessage(), e);
        }
    }

    @Override
    public @Nullable CancellableConnection createCancellableConnection() {
        return new CancellableConnection() {
            CompletableFuture<Map<String, String>> projects;

            @Override
            protected void doTest() throws Exception {
                projects = getProjects();
                projects.get();
            }

            @Override
            public void cancel() {
                if (projects != null) {
                    projects.cancel(true);
                }
            }
        };
    }


    /**
     * Convert WorkItemModel to IntelliJ Task
     */
    private Task convertToTask(WorkItemModel workItemModel) {
        return new Task() {
            @NotNull
            @Override
            public String getId() {
                return String.valueOf(workItemModel.id());
            }

            @Override
            public @NotNull String getNumber() {
                return String.valueOf(workItemModel.id());
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

            @NotNull
            @Override
            public String getPresentableName() {
                return "%s: %s".formatted(getId(), getSummary());
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
                    comments = new Comment[0];
                }
                return comments;
            }

            
            @Override
            public @NotNull Icon getIcon() {
                return switch (getType()) {
                    case BUG -> TasksIcons.Bug;
                    case EXCEPTION -> TasksIcons.Exception;
                    case FEATURE -> AllIcons.Nodes.Favorite;
                    case OTHER -> AllIcons.FileTypes.Any_type;
                    //: EmptyIcon.ICON_0;
                };
                //return TasksIcons.Bug;
            }

            
            @Override
            public @NotNull TaskType getType() {
                final String s = workItemModel.workItemType();
                final TaskType taskType = workItemTypeToTaskTypeMap.getOrDefault(s, TaskType.OTHER);
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
                return project;
            }

            @Nullable
            @Override
            public TaskRepository getRepository() {
                return AzDoRepository.this;
            }

            @Override
            public boolean isClosed() {
                final String state = workItemModel.state();
                return state != null && (state.equalsIgnoreCase("Closed") ||
                                         state.equalsIgnoreCase("Done") ||
                                         state.equalsIgnoreCase("Removed") ||
                                         state.equalsIgnoreCase("Resolved")
                );
            }

            @Override
            public boolean isIssue() {
                return true;
            }

            @Nullable
            @Override
            public java.util.Date getUpdated() {
                return workItemModel.changedDate();
            }

            @Nullable
            @Override
            public java.util.Date getCreated() {
                return workItemModel.createdDate();
            }
        };
    }

    @Override
    public @NotNull @Unmodifiable Set<CustomTaskState> getAvailableTaskStates(@NotNull Task task) throws Exception {
        ensureClient();

        final String id = task.getId();
        final WorkItemModel workItemModel = useClient().getWorkItem(Integer.parseInt(id));
        final String workItemType = workItemModel.workItemType();
        final Map<String, WorkItemType> workItemTypes = useClient().getWorkItemTypes();
        final WorkItemType typeDef = workItemTypes.get(workItemType);
        if (typeDef != null) {
            final Set<CustomTaskState> result = new HashSet<>();
            final Map<String, String> states = typeDef.states();
            if (states != null) {
                for (String state : states.keySet()) {
                    result.add(new CustomTaskState(state, state));
                }
            }
            return result;
        } else {
            return super.getAvailableTaskStates(task);
        }
    }

    @Override
    public void setTaskState(@NotNull Task task, @NotNull CustomTaskState state) throws Exception {
        final String id = task.getId();
        final WorkItemModel workItemModel = useClient().updateWorkItemState(Integer.parseInt(id), state.getId());
        super.setTaskState(task, state);
    }

     
    /**
     * Ensure a client is initialized
     */
    private void ensureClient() {
        AbstractWorkItemClient client1 = useClient();
        if (client1 == null) {
            saveClient(new WorkItemLegacyClient(getOrganization(), getProject(), getPassword()));
            useClient().setTop(top);
//            try {
//                setWorkItemTypes(client.getWorkItemTypes());
//            } catch (AzDException e) {
//                LOG.error("Failed to fetch work item types", e);
//            }
        }
    }

    @Override
    public String getUrl() {
        return AbstractWorkItemClient.toURL(getOrganization());
    }

    private String organization = "";

    @Attribute("organization")
    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
        if (!isEmpty(organization)) {
            String url = WorkItemClient.toURL(this.organization);
            setUrl(url);
        }
        saveClient(null); // Reset client when config changes
    }

    private String project = "";

    @Attribute("project")
    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
        saveClient(null); // Reset client when config changes
    }


    public AzDoRepository map(TaskType taskType, String workItemType) {
        workItemTypeToTaskTypeMap.put(workItemType, taskType);
        taskTypeToWorkItemTypeMap.put(taskType, workItemType);
        return this;
    }

//    public Map<String, WorkItemType> getWorkItemTypes() {
//        return workItemTypes;
//    }

    
    @Attribute("BugWorkItemType")
    public String getBugWorkItemType() {
        return taskTypeToWorkItemTypeMap.get(TaskType.BUG);
    }
    
    public void setBugWorkItemType(String bugWorkItemType) {
        map(TaskType.BUG, bugWorkItemType);
    }
    
    @Attribute("FeatureWorkItemType")
    public String getFeatureWorkItemType() {
        return taskTypeToWorkItemTypeMap.get(TaskType.FEATURE);
    }
    public void setFeatureWorkItemType(String featureWorkItemType) {
        map(TaskType.FEATURE, featureWorkItemType);
    }
    private String team = "";

    @Attribute("team")
    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
        saveClient(null);
    }

    private int top = 100;

    @Attribute("top")
    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
        saveClient(null);
    }

    @Override
    public void setPassword(String password) {
        super.setPassword(password);
        saveClient(null); // Reset client when config changes
    }

    @Override
    public boolean isConfigured() {
        return canBeAccessed()
                && !isEmpty(getProject())
                ;
    }

    public boolean canBeAccessed() {
        return !isEmpty(getOrganization())
                && !isEmpty(getPassword());
    }

    @Override
    protected int getFeatures() {
        return super.getFeatures() | STATE_UPDATING ;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AzDoRepository that = (AzDoRepository) o;
        return Objects.equals(getOrganization(), that.getOrganization())
               && Objects.equals(getProject(), that.getProject())
               && Objects.equals(getTeam(), that.getTeam())
               && Objects.equals(getPassword(), that.getPassword())
               && Objects.equals(getTop(), that.getTop())
               && Objects.equals(getBugWorkItemType(), that.getBugWorkItemType())
               && Objects.equals(getFeatureWorkItemType(), that.getFeatureWorkItemType())
                ;

    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrganization(), getProject(), getTeam(), getPassword(), getTop(),getBugWorkItemType(),getFeatureWorkItemType());
    }
}
