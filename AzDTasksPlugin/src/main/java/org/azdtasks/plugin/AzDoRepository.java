package org.azdtasks.plugin;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.httpclient.NewBaseRepositoryImpl;
import com.intellij.ui.IconManager;
import com.intellij.util.IconUtil;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.apache.commons.lang3.math.NumberUtils;
import org.azd.exceptions.AzDException;

import org.azdtasks.core.*;
import org.azdtasks.core.client.AbstractWorkItemClient;
import org.azdtasks.core.client.WorkItemClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Azure DevOps task repository implementation
 */
@Tag(AzDoRepositoryType.NAME)
public class AzDoRepository extends NewBaseRepositoryImpl {

    private static final Logger LOG = Logger.getInstance(AzDoRepository.class);



    //Categories are sorted by their logical order
    public enum Category {

        Proposed(), InProgress(), Resolved(), Completed(true), Removed(true);
        private final boolean isClosed;

        public static Comparator<Category> getComparator() {
            return Comparator.comparing(Category::isClosed).thenComparing(Enum::ordinal);
        }

        Category() {
            this(false);
        }

        Category(boolean isClosed) {
            this.isClosed = isClosed;
        }

        public boolean isClosed() {
            return isClosed;
        }

    }

    private final Set<String> defClosedStates = Set.of("Closed", "Done", "Removed", "Resolved");
    private Map<String, WorkItemType> workItemTypes = new HashMap<>();
//    private final Map<TaskType, String> taskTypeToWorkItemTypeMap = new HashMap<>();
//    private final Map<String, TaskType> workItemTypeToTaskTypeMap = new HashMap<>();
    //Microsoft.RequirementCategory
    //Microsoft.TaskCategory
    private final Map<String,TaskType> categoryToTaskType=Map.of(
            "Microsoft.BugCategory",TaskType.BUG
        ,"Microsoft.FeatureCategory",TaskType.FEATURE
        ,"",TaskType.OTHER
);
    private Map<String, WorkItemTypeLinkToCategory> typesToCategories=new HashMap<>();

    private transient AbstractWorkItemClient client = null;


    //required for reflection
    public AzDoRepository() {
        super();
//        setBugWorkItemType("Bug");
//        setFeatureWorkItemType("Feature");
    }

    public AzDoRepository(TaskRepositoryType type) {
        super(type);
//        setBugWorkItemType("Bug");
//        setFeatureWorkItemType("Feature");
    }

    private AzDoRepository(AzDoRepository other) {
        super(other);
        LOG.info("Cloning AzDoRepository");
        setOrganization(other.getOrganization());
        setProject(other.getProject());
        setTeam(other.getTeam());
        setTop(other.getTop());
        setTimeTrackFieldName(other.getTimeTrackFieldName());
//        workItemTypeToTaskTypeMap.putAll(other.workItemTypeToTaskTypeMap);
//        taskTypeToWorkItemTypeMap.putAll(other.taskTypeToWorkItemTypeMap);
    }

    @NotNull
    @Override
    public BaseRepository clone() {
        return new AzDoRepository(this);
    }

    public boolean containsState(String state) {
        return defClosedStates.contains(state);
    }

    private final Map<String,Icon> cIcons=new HashMap<>();
    public Optional<Icon> fetchIcon(String url){
        Icon icon = cIcons.get(url);
        return Optional.ofNullable(icon);
    }
    /**
     * Ensure a client is initialized
     */
    private synchronized AbstractWorkItemClient fetchClient() {
        final AbstractWorkItemClient localClient = this.client;
        if (localClient == null) {
            final AbstractWorkItemClient c = createClient();
            try {
                workItemTypes = c.getWorkItemTypes();
                final Collection<WorkItemType> values = workItemTypes.values();
                for (WorkItemType value : values) {
                    String s = value.iconUrl();
                    try {
                        final URL url = new URL(s);
                        final Icon icon = IconLoader.findIcon(url, true);
                        if (icon!=null) {
                            final Icon size = IconUtil.scale(icon, null, 20f / icon.getIconWidth());
                            cIcons.put(s, size);
                        }
                    }catch (MalformedURLException e){
                        LOG.error("Failed getting workItems", e);
                    }
                }
                typesToCategories = c.getTypesToCategories();
            } catch (WorkItemException e) {
                LOG.error("Failed getting workItems", e);
            }
            setClient(c);
            return c;
        } else {
            return localClient;
        }
    }

    private synchronized void setClient(AbstractWorkItemClient client) {
        this.client = client;
    }

    private void clearClient() {
        setClient(null);
    }

    private @NotNull AbstractWorkItemClient createClient() {
        final String project = getProject();
        return createClient(project);
    }

    private @NotNull AbstractWorkItemClient createClient(String project) {
        final AbstractWorkItemClient client = new WorkItemClient(getOrganization(), project, getPassword());
        client.setTop(top);
        return client;
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


    @Override
    public boolean isConfigured() {
        return canBeAccessed()
                && StringUtil.isNotEmpty(getProject())
                ;
    }

    public boolean canBeAccessed() {
        return StringUtil.isNotEmpty(getOrganization())
                && StringUtil.isNotEmpty(getPassword());
    }

    public CompletableFuture<Map<String, String>> getProjects() {
        if (canBeAccessed()) {
            final AbstractWorkItemClient abstractWorkItemClient = createClient();
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
        if (canBeAccessed()) {
            final AbstractWorkItemClient abstractWorkItemClient = createClient();
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

    @Override
    public @NlsContexts.Label String getPresentableName() {
        final String url = getUrl();
        if (StringUtil.isNotEmpty(url)) {
            final String project = getProject();
            final String team = getTeam();
            String name = url;
            if (StringUtil.isNotEmpty(project)) {
                name += " - " + project;
            }
            if (StringUtil.isNotEmpty(team)) {
                name += " - " + team;
            }
            return name;
        } else {
            return TaskApiBundle.message("label.undefined");
        }
    }

    public @NotNull Map<String, String> getWorkItemFieldsForTimeTrack() throws WorkItemException {
        if (canBeAccessed()) {
            final AbstractWorkItemClient client = createClient();
            final Map<String, WorkItemField> fields = client.getWorkItemFields(v -> v.isSystem() && v.type().equals("DOUBLE") && !v.isReadOnly());

            final Map<String, String> m = fields.values()
                    .stream()
                    .collect(Collectors.toMap(WorkItemField::id, WorkItemField::name));
            m.put("", "");
            return m;
        } else {
            LOG.error("Not Configured");
            return Map.of();
        }
    }


    public boolean isTaskClosed(String state, final WorkItemType workItemType) {
        if (state != null) {
            if (workItemType != null) {
                final Map<String, String> states = workItemType.states();
                if (states != null) {
                    final String cat = states.getOrDefault(state, "");
                    if (!cat.isEmpty()) {
                        final AzDoRepository.Category category = AzDoRepository.Category.valueOf(cat);
                        return category.isClosed();
                    } else {
                        return containsState(state);
                    }
                } else {
                    return containsState(state);
                }
            } else {
                return containsState(state);
            }
        } else {
            return false;
        }
    }

    public @NotNull Map<String, String> getWorkItemTypes(String project) throws WorkItemException {
        if (canBeAccessed() && StringUtil.isNotEmpty(project)) {
            final AbstractWorkItemClient client = createClient(project);
            final Map<String, WorkItemType> workItemTypes1 = client.getWorkItemTypes();
            final Map<String, String> m = workItemTypes1.keySet()
                    .stream()
                    .collect(Collectors.toMap(s -> s, s -> s));
            return m;
        } else {
            LOG.error("Not Configured");
            return Map.of();
        }
    }


    @Override
    public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed, @NotNull ProgressIndicator cancelled) throws Exception {
        try {
            final List<WorkItemModel> workItems;
            if (StringUtil.isEmpty(query)) {
                workItems = List.of();
            } else {
                if (NumberUtils.isParsable(query)) {
                    final int id = Integer.parseInt(query);
                    final WorkItemModel workItem = fetchClient().getWorkItem(id);
                    workItems = workItem != null ? List.of(workItem) : List.of();
                } else {
                    final String formatted = getQuery(query);
                    workItems = getExecuteQuery(formatted);
                }
            }
            return convert(workItems);
        } catch (WorkItemException e) {
            LOG.error("Failed to fetch work items", e);
            throw new Exception("Failed to fetch work items: " + e.getMessage(), e);
        }
    }

    private AzTask @NotNull [] convert(List<WorkItemModel> workItems) {
        return workItems.parallelStream()
                .map(this::convertToTask)
                .toArray(AzTask[]::new);
    }

    private List<WorkItemModel> getExecuteQuery(String formatted) throws WorkItemException {
        return fetchClient().executeQuery(getTeam(), formatted);
    }

    public AzTask @NotNull [] getCurrentTasks() throws WorkItemException {
        final String s = """
                SELECT [System.Id]
                FROM WorkItems 
                WHERE System.IterationPath=@CurrentIteration AND [System.AssignedTo] = @Me 
                ORDER BY Microsoft.VSTS.Common.Priority, System.State
                """;
        final List<WorkItemModel> executeQuery = getExecuteQuery(s);
        return convert(executeQuery);
    }

    private @NotNull String getQuery(@NotNull String query) {
        final String s = """
                SELECT [System.Id]
                FROM WorkItems
                WHERE [System.TeamProject] = '%s' AND ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s')
                AND (
                           [System.WorkItemType] IN GROUP 'Microsoft.BugCategory'
                        OR [System.WorkItemType] IN GROUP 'Microsoft.TaskCategory'
                        OR [System.WorkItemType] IN GROUP 'Microsoft.RequirementCategory'
                    )
                ORDER BY [System.ChangedDate] DESC
                """;
        final String escapedSearch = query.replace("'", "''");

        final String formatted = s.formatted(getProject(), escapedSearch, escapedSearch);
        return formatted;
    }


    @Nullable
    @Override
    public Task findTask(@NotNull String id) throws Exception {
        try {
            if (NumberUtils.isParsable(id)) {
                final int workItemId = Integer.parseInt(id);
                final WorkItemModel workItem = fetchClient().getWorkItem(workItemId);
                final Task task = convertToTask(workItem);
                return task;
            } else {
                return null;
            }
        } catch (AzDException e) {
            LOG.error("Failed to find work item " + id, e);
            throw new Exception("Failed to find work item: " + e.getMessage(), e);
        }
    }

    private final static String delim = "-";

    public String buildId(String id) {
        final String formatted = "%s %s%s%s".formatted(getRepositoryType().getName(), getProject(), delim, id);
        return formatted;

    }

    @Override
    public @Nullable String extractId(@NotNull String taskName) {
        if (taskName.startsWith(getRepositoryType().getName())) {
            int i = taskName.lastIndexOf(delim);
            final String s = i > 0 ? taskName.substring(i + 1) : taskName;
            return s;
        } else {
            return null;
        }
    }

    public int parseNumberFromTaskNumber(Task task) {
        final String number = task.getNumber();
        return Integer.parseInt(number);
    }


    private AzTask convertToTask(WorkItemModel workItemModel) {
        final String key = workItemModel.workItemType();

        final WorkItemType workItemType = workItemTypes.get(key);
        final WorkItemTypeLinkToCategory workItemTypeLinkToCategory = typesToCategories.get(key);
        final String s = workItemTypeLinkToCategory.workItemTypeCategory().refName();
        final TaskType taskType = categoryToTaskType.getOrDefault(s, TaskType.OTHER);
        final String s1 = workItemType.iconUrl();
        return new AzTask(this
                , taskType
                , isTaskClosed(workItemModel.state(), workItemType)
                , workItemModel,s1);
    }


    @Override
    public @NotNull @Unmodifiable Set<CustomTaskState> getAvailableTaskStates(@NotNull Task task) throws Exception {
        final AbstractWorkItemClient client = fetchClient();
        final int id = parseNumberFromTaskNumber(task);
        final WorkItemModel workItemModel = client.getWorkItem(id);
        final String workItemType = workItemModel.workItemType();
        final WorkItemType typeDef = workItemTypes.get(workItemType);
        if (typeDef != null) {
            final Set<CustomTaskState> result = new LinkedHashSet<>();
            final Map<String, String> states = typeDef.states();
            if (states != null) {
                final List<Map.Entry<String, String>> stateEntries = new ArrayList<>(states.entrySet());
                try {
                    stateEntries.sort((o1, o2) -> {
                        String value = o1.getValue();
                        String value1 = o2.getValue();
                        Category category = Category.valueOf(value);
                        Category category1 = Category.valueOf(value1);
                        return Category.getComparator().compare(category, category1);
                    });
                } catch (Exception e) {
                    LOG.error("Failed to sort states", e);
                }
                for (Map.Entry<String, String> entry : stateEntries) {
                    final String state = entry.getKey();
                    final String category = entry.getValue();
                    final CustomTaskState e = new CustomTaskState(state, "%s (%s)".formatted(state, category));
                    result.add(e);
                }
            }
            return result;
        } else {
            return super.getAvailableTaskStates(task);
        }
    }

    @Override
    public void setTaskState(@NotNull Task task, @NotNull CustomTaskState state) throws Exception {
        final int id = parseNumberFromTaskNumber(task);
        final WorkItemModel workItemModel = fetchClient().updateWorkItemState(id, state.getId());
        super.setTaskState(task, state);
    }


    private Optional<Double> getTimeSpent(String timeSpent) {
        final Matcher matcher = TIME_SPENT_PATTERN.matcher(timeSpent);
        if (matcher.find()) {
            final int hours = Integer.parseInt(matcher.group(1));
            final int minutes = Integer.parseInt(matcher.group(2));
            final int totalMinutes = hours * 60 + minutes;
            final double value = totalMinutes / (double) 60;
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void updateTimeSpent(@NotNull LocalTask task, @NotNull String timeSpent, @NotNull String comment) throws Exception {
        final int id = parseNumberFromTaskNumber(task);
        if (!timeTrackFieldName.isEmpty()) {
            getTimeSpent(timeSpent).ifPresentOrElse(v -> {
                if (v > 0) {
                    try {
                        fetchClient().updateWorkItem(id, timeTrackFieldName, "%.2f".formatted(v));
                    } catch (WorkItemException e) {
                        LOG.error("Error updating task ", e);
                    }
                }
            }, () -> LOG.error("%s does not conform to the pattern ".formatted(timeSpent)));
        }
        if (!comment.isEmpty()) {
            fetchClient().addWorkItemComment(id, comment);
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
        if (StringUtil.isNotEmpty(organization)) {
            final String url = WorkItemClient.toURL(this.organization);
            setUrl(url);
        }
        clearClient();
    }

    private String project = "";

    @Attribute("project")
    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
        clearClient();
    }


    public AzDoRepository map(TaskType taskType, String workItemType) {
//        workItemTypeToTaskTypeMap.put(workItemType, taskType);
//        taskTypeToWorkItemTypeMap.put(taskType, workItemType);
        return this;
    }


//    @Attribute("BugWorkItemType")
//    public String getBugWorkItemType() {
//        return taskTypeToWorkItemTypeMap.get(TaskType.BUG);
//    }
//
//    public void setBugWorkItemType(String bugWorkItemType) {
//        map(TaskType.BUG, bugWorkItemType);
//    }

//    @Attribute("FeatureWorkItemType")
//    public String getFeatureWorkItemType() {
//        return taskTypeToWorkItemTypeMap.get(TaskType.FEATURE);
//    }
//
//    public void setFeatureWorkItemType(String featureWorkItemType) {
//        final AzDoRepository map = map(TaskType.FEATURE, featureWorkItemType);
//    }

    private String team = "";

    @Attribute("team")
    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
        clearClient();
    }

    public String timeTrackFieldName = "";

    @Attribute("TimeTrackingFieldName")
    public String getTimeTrackFieldName() {
        return timeTrackFieldName;
    }

    public void setTimeTrackFieldName(String timeTrackFieldName) {
        this.timeTrackFieldName = timeTrackFieldName;
        clearClient();
    }

    private int top = 100;

    @Attribute("top")
    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
        clearClient();
    }

    @Override
    public void setPassword(String password) {
        super.setPassword(password);
        clearClient();
    }


    @Override
    protected int getFeatures() {
        return super.getFeatures() | STATE_UPDATING | TIME_MANAGEMENT;
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
//                && Objects.equals(getBugWorkItemType(), that.getBugWorkItemType())
//                && Objects.equals(getFeatureWorkItemType(), that.getFeatureWorkItemType())
                && Objects.equals(getTimeTrackFieldName(), that.getTimeTrackFieldName())
                ;

    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrganization()
                , getProject()
                , getTeam()
                , getTop()
//                , getBugWorkItemType()
//                , getFeatureWorkItemType()
                , getTimeTrackFieldName());
    }

}
