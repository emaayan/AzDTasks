package org.azdtasks.plugin;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.httpclient.NewBaseRepositoryImpl;
import com.intellij.util.Consumer;
import com.intellij.util.IconUtil;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.apache.commons.lang3.math.NumberUtils;

import org.azdtasks.core.*;
import org.azdtasks.core.client.WorkItemClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.tasklist.plugin.TaskQueryProvider;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Azure DevOps task repository implementation
 */
@Tag(AzDoRepositoryType.NAME)
public class AzDoRepository extends NewBaseRepositoryImpl implements TaskQueryProvider<AzTask> {

    private static final Logger LOG = Logger.getInstance(AzDoRepository.class);

    public static final String DEFAULT_WHERE = "System.IterationPath=@CurrentIteration AND [System.AssignedTo] = @Me";


    //Categories are sorted by their logical order
    public enum Category {

        Proposed(), InProgress(), Resolved(), Completed(true), Removed(true);
        private final boolean isClosed;

        public static Comparator<Category> getComparator() {
            return Comparator.comparing(Category::isClosed).thenComparing(Enum::ordinal);
        }

        private static int compare(String cat, String cat1) {
            final Category category = Category.valueOf(cat);
            final Category category1 = Category.valueOf(cat1);
            return getComparator().compare(category, category1);
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

    private static final Set<String> defClosedStates = Set.of("Closed", "Done", "Removed", "Resolved");


    private static final String s = """
            SELECT [System.Id]
            FROM WorkItems
            WHERE [System.TeamProject] = '%s' AND ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s')
            AND (
                       [System.WorkItemType] IN GROUP 'Microsoft.BugCategory'
                    OR [System.WorkItemType] IN GROUP 'Microsoft.TaskCategory'
                    OR [System.WorkItemType] IN GROUP 'Microsoft.RequirementCategory'
                    OR [System.WorkItemType] IN GROUP 'Microsoft.FeatureCategory'
                )
            ORDER BY [System.ChangedDate] DESC
            """;
    private static final Map<String, TaskType> categoryToTaskType = Map.of(
              "Microsoft.BugCategory", TaskType.BUG
            , "Microsoft.TaskCategory", TaskType.FEATURE
            , "Microsoft.RequirementCategory", TaskType.FEATURE
            , "Microsoft.FeatureCategory", TaskType.FEATURE
            , "", TaskType.OTHER
    );


    private record RepoContext(Map<String, WorkItemTypeLinkToCategory> typesToCategories, Map<String, Icon> cIcons) {

    }

    private transient WorkItemClient client = null;
    private transient RepoContext repoContext = null;
    private String where = "";

    //required for reflection
    public AzDoRepository() {
        super();
        setWhere(AzDoRepository.DEFAULT_WHERE);
    }

    public AzDoRepository(TaskRepositoryType type) {
        super(type);
        setWhere(AzDoRepository.DEFAULT_WHERE);
    }

    private AzDoRepository(AzDoRepository other) {
        super(other);
        LOG.info("Cloning AzDoRepository");
        setOrganization(other.getOrganization());
        setProject(other.getProject());
        setTeam(other.getTeam());
        setTop(other.getTop());
        setTimeTrackFieldName(other.getTimeTrackFieldName());
        setWhere(other.getWhere());
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
                && Objects.equals(getTimeTrackFieldName(), that.getTimeTrackFieldName())
                && Objects.equals(getWhere(), that.getWhere())
                ;

    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getOrganization()
                , getProject()
                , getTeam()
                , getPassword()
                , getTop()
                , getTimeTrackFieldName()
                , getWhere()
        )
                ;
    }

    @NotNull
    @Override
    public BaseRepository clone() {
        return new AzDoRepository(this);
    }

    public boolean containsState(String state) {
        return defClosedStates.contains(state);
    }


    public Optional<Icon> fetchIcon(String url) {
        Icon icon = repoContext.cIcons().get(url);
        return Optional.ofNullable(icon);
    }

    /**
     * Ensure a client is initialized
     */
    private synchronized WorkItemClient fetchClient() {
        final WorkItemClient localClient = this.client;
        if (localClient == null) {
            LOG.info("Init client");
            try {
                final WorkItemClient c = initClient();
                final RepoContext repoContext2 = getRepoContext(c);
                setClient(c, repoContext2);
                return c;
            } catch (Exception e) {
                LOG.error("Failed init Client ", e);
                throw new RuntimeException(e);
            }
        } else {
            return localClient;
        }
    }

    private @NotNull WorkItemClient initClient() throws WorkItemException {
        LOG.info("Init Az Client");
        final WorkItemClient c = createClient();
        LOG.info("DONE INIT Az Client");
        return c;
    }

    private Optional<Icon> createIcon(String s) {
        try {
            final URL url = new URL(s);
            final Icon icon = IconLoader.findIcon(url, true);
            if (icon != null) {
                final Icon size = IconUtil.scale(icon, null, 20f / icon.getIconWidth());
                return Optional.of(size);
            } else {
                return Optional.empty();
            }
        } catch (MalformedURLException e) {
            LOG.error("Failed getting workItems icon", e);
            return Optional.empty();
        }

    }

    private @NotNull RepoContext getRepoContext(WorkItemClient c) {
        if (canBeAccessed()) {
            try {
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return c.getTypesToCategories();
                    } catch (WorkItemException e) {
                        throw new CompletionException(e);
                    }
                }).exceptionally(throwable -> Map.of());

                final Map<String, WorkItemTypeLinkToCategory> typesToCategories = c.getTypesToCategories();
                final Collection<WorkItemTypeLinkToCategory> values = typesToCategories.values();
                LOG.info("Getting States");
                final Map<String, Icon> cIcons = new HashMap<>();
                for (WorkItemTypeLinkToCategory value : values) {
                    final WorkItemType workItemType = value.workItemType();
                    final String s = workItemType.iconUrl();
                    final Optional<Icon> icon1 = createIcon(s);
                    icon1.ifPresent((Consumer<Icon>) icon -> cIcons.put(s, icon));
                }
                final RepoContext repoContext = new RepoContext(typesToCategories, cIcons);
                return repoContext;
            } catch (WorkItemException e) {
                LOG.error("Failed init context ", e);
                return new RepoContext(Map.of(), Map.of());
            }
        } else {
            return new RepoContext(Map.of(), Map.of());
        }
    }

    private synchronized void setClient(WorkItemClient client, RepoContext repoContext) {
        this.client = client;
        this.repoContext = repoContext;
    }

    private void clearClient() {
        setClient(null, null);
    }

    public @NotNull WorkItemClient createClient() throws WorkItemException {
        final String project = getProject();
        final WorkItemClient client = new WorkItemClient(getOrganization(), project, getPassword());
        client.setTop(top);
        return client;
    }

    @Override
    public @Nullable CancellableConnection createCancellableConnection() {
        return new CancellableConnection() {
            String user = "";

            @Override
            protected void doTest() throws Exception {
                WorkItemClient workItemClient = fetchClient();
                user = workItemClient.getCurrentUser();
                //projects = getProjects();
                //projects.get();
            }

            @Override
            public void cancel() {
                user = "";
            }
        };
    }

    public record AzEditorContext(Map<String, String> projects, Map<String, String> teams,
                                  Map<String, String> fieldsForTimeTrack) {

    }

    private Supplier<Map<String, String>> call(Callable<Map<String, String>> c) {
        return () -> {
            try {
                return c.call();
            } catch (Exception e) {
                LOG.error("Error", e);
                return Map.of();
            }
        };
    }

    public synchronized AzEditorContext create() throws WorkItemException {
        final WorkItemClient client1 = createClient();


        CompletableFuture<Map<String, String>> projects = CompletableFuture.supplyAsync(call(client1::getProjects));
        CompletableFuture<Map<String, String>> teams = CompletableFuture.supplyAsync(call(client1::getTeams));
        CompletableFuture<Map<String, String>> fields = CompletableFuture.supplyAsync(call(this::getWorkItemFieldsForTimeTrack));
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(projects, teams, fields);
        final Void join = voidCompletableFuture.join();
        return null;
    }

    public Map<String, String> getProjects() throws WorkItemException {
        final WorkItemClient client = fetchClient();// createClient();
        return client.getProjects();
    }

    public Map<String, String> getTeams() throws WorkItemException {
        final WorkItemClient client = fetchClient();// createClient();
        return client.getTeams();
    }

    public @NotNull Map<String, String> getWorkItemFieldsForTimeTrack() throws WorkItemException {
        final WorkItemClient client = createClient();
        return getWorkItemFieldsForTimeTrack(client);
    }

    public static @NotNull Map<String, String> getWorkItemFieldsForTimeTrack(WorkItemClient client) throws WorkItemException {
        final Map<String, WorkItemField> fields = client.getWorkItemFields(v -> v.isSystem() && v.type().equals("DOUBLE") && !v.isReadOnly());
        final Map<String, String> m = fields.values()
                .stream()
                .collect(Collectors.toMap(WorkItemField::id, WorkItemField::name));
        m.put("", "");
        return m;
    }

    @Override
    public boolean isConfigured() {
        return super.isConfigured() && canBeAccessed() && StringUtil.isNotEmpty(getProject());
    }

    public boolean canBeAccessed() {
        return StringUtil.isNotEmpty(getOrganization()) && StringUtil.isNotEmpty(getPassword());
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
                    if (workItem!=null) {
                        final TaskType taskTypeFromWorkItemType = getTaskTypeFromWorkItemType(workItem.workItemType());
                        workItems =  TaskType.OTHER!=taskTypeFromWorkItemType ? List.of(workItem) : List.of();
                    }else{
                        workItems = List.of();
                    }
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

    @Override
    public AzTask @NotNull [] getCurrentTasks() throws WorkItemException {
        return getTasks(where);
    }

    public AzTask @NotNull [] getTasks(final String whereClause) throws WorkItemException {
        final String w = " " + wrapWhere(whereClause) + " ";//add space to avoid syntax error
        final String s = """
                SELECT [System.Id]
                FROM WorkItems
                %s
                ORDER BY Microsoft.VSTS.Common.Priority, System.State
                """.formatted(w);
        final List<WorkItemModel> executeQuery = getExecuteQuery(s);
        return convert(executeQuery);
    }

    //provide escaping if needed
    protected @NotNull String wrapWhere(String w) {
        return StringUtil.isNotEmpty(w) ? "WHERE " + w : " WHERE " + AzDoRepository.DEFAULT_WHERE;
    }


    private @NotNull String getQuery(@NotNull String query) {

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
                if (workItem != null) {
                    final Task task = convertToTask(workItem);
                    return task;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (WorkItemException e) {
            LOG.error("Failed to find work item " + id, e);
            throw e;
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
        final String workItemType = workItemModel.workItemType();
        final Optional<WorkItemType> workItemTypeDefOpt = getWorkItemTypeDef(workItemType);
        if (workItemTypeDefOpt.isPresent()) {
            final WorkItemType workItemTypeDef = workItemTypeDefOpt.get();
            final TaskType taskType = getTaskTypeFromWorkItemType(workItemType);
            final String iconURL = workItemTypeDef.iconUrl();
            return new AzTask(this
                    , taskType
                    , isTaskClosed(workItemModel.state(), workItemTypeDef)
                    , workItemModel, iconURL);
        } else {
            return null;
        }
    }

    private TaskType getTaskTypeFromWorkItemType(String workItemType) {
        final WorkItemTypeLinkToCategory workItemTypeLinkToCategory = repoContext.typesToCategories().get(workItemType);
        final String s = workItemTypeLinkToCategory.workItemTypeCategory().refName();
        final TaskType taskType = categoryToTaskType.getOrDefault(s, TaskType.OTHER);
        return taskType;
    }


    @Override
    public @NotNull @Unmodifiable Set<CustomTaskState> getAvailableTaskStates(@NotNull Task task) throws Exception {
        final WorkItemClient client = fetchClient();
        final int id = parseNumberFromTaskNumber(task);
        final WorkItemModel workItemModel = client.getWorkItem(id);
        if (workItemModel != null) {
            final String workItemType = workItemModel.workItemType();
            //final Set<CustomTaskState> customTaskStates = workItemTypeStates.get(workItemType);
            final Optional<WorkItemType> workItemTypeDef = getWorkItemTypeDef(workItemType);
            if (workItemTypeDef.isPresent()) {
                return getWorkItemStates(workItemTypeDef.get());
            } else {
                return super.getAvailableTaskStates(task);
            }
        } else {
            return super.getAvailableTaskStates(task);
        }
    }

    private Optional<WorkItemType> getWorkItemTypeDef(String workItemType) {
        final WorkItemTypeLinkToCategory workItemTypeLinkToCategory = repoContext.typesToCategories().get(workItemType);
        if (workItemTypeLinkToCategory != null) {
            return Optional.ofNullable(workItemTypeLinkToCategory.workItemType());
        } else {
            return Optional.empty();
        }
    }

    private @NotNull Set<CustomTaskState> getWorkItemStates(WorkItemType typeDef) {
        final Set<CustomTaskState> result = new LinkedHashSet<>();
        final Map<String, String> states = typeDef.states();
        if (states != null) {
            final List<Map.Entry<String, String>> stateEntries = new ArrayList<>(states.entrySet());
            try {
                stateEntries.sort((o1, o2) -> Category.compare(o1.getValue(), o2.getValue()));
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
        return WorkItemClient.toURL(getOrganization());
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
    public boolean isReady() {
        return isConfigured() && StringUtil.isNotEmpty(where);
    }

    @Attribute("where")
    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
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

}
