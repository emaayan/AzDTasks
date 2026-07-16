package org.azdtasks.core.client;

import org.azd.abstractions.ApiResponse;
import org.azd.abstractions.serializer.SerializableEntity;
import org.azd.authentication.AccessTokenCredential;
import org.azd.authentication.PersonalAccessTokenCredential;
import org.azd.common.ApiVersion;
import org.azd.common.Constants;
import org.azd.common.ResourceId;
import org.azd.common.types.Author;
import org.azd.common.types.JsonPatchDocument;
import org.azd.core.CoreRequestBuilder;
import org.azd.core.projects.ProjectsRequestBuilder;
import org.azd.core.teams.TeamsRequestBuilder;
import org.azd.core.types.*;
import org.azd.enums.*;
import org.azd.exceptions.AzDException;
import org.azd.http.ClientRequest;
import org.azd.locations.LocationsBaseRequestBuilder;
import org.azd.locations.types.AuthenticatedUser;
import org.azd.locations.types.ConnectionData;
import org.azd.serviceclient.AzDService;
import org.azd.serviceclient.AzDServiceClient;
import org.azd.utils.StringUtils;
import org.azd.utils.UrlBuilder;
import org.azd.workitemtracking.WorkItemTrackingRequestBuilder;
import org.azd.workitemtracking.comments.CommentsRequestBuilder;
import org.azd.workitemtracking.fields.FieldsRequestBuilder;
import org.azd.workitemtracking.types.*;
import org.azd.workitemtracking.types.WorkItemTypeCategory;
import org.azd.workitemtracking.wiql.WiqlRequestBuilder;
import org.azd.workitemtracking.workitems.WorkItemsRequestBuilder;
import org.azd.workitemtracking.workitemtypecategories.WorkItemTypeCategoriesRequestBuilder;
import org.azd.workitemtracking.workitemtypes.WorkItemTypesRequestBuilder;
import org.azdtasks.core.*;

import org.azdtasks.core.WorkItemField;
import org.azdtasks.core.WorkItemType;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.*;
import java.util.stream.Collectors;

public class WorkItemClient {

    private final static Logger LOG = LoggerFactory.getLogger(WorkItemClient.class);


    private static AzDServiceClient getClient(String org, String project, String personalAccessToken) {
        final PersonalAccessTokenCredential pat = new PersonalAccessTokenCredential(toURL(org), project, personalAccessToken);
        final AzDServiceClient service = AzDService.builder().authentication(pat).buildClient();
        return service;
    }


    public static String toURL(String org) {
        return Instance.BASE_INSTANCE.append(org);
    }

    private final String organization;
    private final String personalAccessToken;
    private final String project;

    private final HtmlCleaner htmlCleaner;

    private final AzDServiceClient azDServiceClient;
    private final ProjectsRequestBuilder projects;
    private final TeamsRequestBuilder teams;
    private final ConnectionData connectionData;

    private final CommentsRequestBuilder commentsRequestBuilder;
    private final WorkItemsRequestBuilder workItemsRequestBuilder;
    private final LocationsBaseRequestBuilder locationsBaseRequestBuilder;
    private final FieldsRequestBuilder fieldsRequestBuilder;
    private final WorkItemTypesRequestBuilder workItemTypesRequestBuilder;
    private final WiqlRequestBuilder wiql;
    private final WorkItemTypeCategoriesRequestBuilder workItemTypeCategoriesRequestBuilder;


    public WorkItemClient(String organization, String personalAccessToken) throws WorkItemException {
        this(organization, null, personalAccessToken);
    }

    public WorkItemClient(String organization, String project, String personalAccessToken) throws WorkItemException {

        this.organization = organization;
        this.personalAccessToken = personalAccessToken;
        this.project = project == null ? "" : project;
        azDServiceClient = getClient(organization, getProject(), personalAccessToken);
        locationsBaseRequestBuilder = azDServiceClient.locations();
        connectionData = execute(locationsBaseRequestBuilder::getConnectionData, Function.identity());

        final CoreRequestBuilder core = azDServiceClient.core();
        projects = core.projects();
        teams = core.teams();

        final WorkItemTrackingRequestBuilder wit = azDServiceClient.workItemTracking();
        commentsRequestBuilder = wit.comments();
        workItemsRequestBuilder = wit.workItems();
        workItemTypesRequestBuilder = wit.workItemTypes();
        workItemTypeCategoriesRequestBuilder = wit.workItemTypeCategories();
        fieldsRequestBuilder = wit.fields();
        wiql = wit.wiql();


        final CleanerProperties properties = new CleanerProperties();
        htmlCleaner = new HtmlCleaner(properties);

    }

    public String getOrganization() {
        return organization;
    }

    public String getPersonalAccessToken() {
        return personalAccessToken;
    }

    public String getProject() {
        return project;
    }

    public String getCurrentUser() {
        final AuthenticatedUser authenticatedUser = connectionData.getAuthenticatedUser();
        return authenticatedUser.getProviderDisplayName();
    }

    private int top = 50;

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    protected boolean timePrecision = false;

    public boolean isTimePrecision() {
        return timePrecision;
    }

    public void setTimePrecision(boolean timePrecision) {
        this.timePrecision = timePrecision;
    }


    private String get(Map m, String name, String defValue) {
        final Object orDefault = m.getOrDefault(name, defValue);
        final Object o = Objects.requireNonNullElse(orDefault, defValue);
        return o.toString();
    }

    protected List<org.azd.workitemtracking.types.WorkItemField> getWorkItemFieldsImpl(GetFieldsExpand getFieldsExpand) throws WorkItemException {
        return execute(() -> fieldsRequestBuilder.list(getFieldsExpand), list -> {
            final Map<String, Object> otherFields = list.getOtherFields();
            final List<Object> values = (List<Object>) otherFields.getOrDefault("value", List.of());
            final List<org.azd.workitemtracking.types.WorkItemField> fields = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof Map m) {
                    final String id = get(m, "referenceName", "");
                    final String name = get(m, "name", "");
                    final String description = get(m, "description", "");
                    final String type = get(m, "type", "");
                    final String usage = get(m, "usage", "");
                    final boolean readOnly = Boolean.parseBoolean(get(m, "readOnly", Boolean.FALSE.toString()));
                    final org.azd.workitemtracking.types.WorkItemField workItemField = new org.azd.workitemtracking.types.WorkItemField();
                    workItemField.setReferenceName(id);
                    workItemField.setDescription(description);
                    workItemField.setName(name);
                    final FieldUsage fieldUsage = FieldUsage.valueOf(usage.toUpperCase());
                    workItemField.setUsage(fieldUsage);
                    final FieldType fieldType = FieldType.valueOf(type.toUpperCase());
                    workItemField.setType(fieldType);
                    workItemField.setReadOnly(readOnly);
                    fields.add(workItemField);
                }
            }
            return fields;
        });
    }

    public Map<String, WorkItemField> getWorkItemFields(Predicate<WorkItemField> filter) throws WorkItemException {

        final List<org.azd.workitemtracking.types.WorkItemField> workItemFieldsImpl = getWorkItemFieldsImpl(GetFieldsExpand.EXTENSIONFIELDS);
        final Map<String, WorkItemField> m = new HashMap<>();
        workItemFieldsImpl.forEach(workItemField -> {
            final FieldUsage usage = workItemField.getUsage();
            if (FieldUsage.WORKITEM.equals(usage)) {
                final String referenceName = workItemField.getReferenceName();
                final String name = workItemField.getName();
                final FieldType type = workItemField.getType();
                final boolean readOnly = workItemField.getReadOnly();
                final String typeName = type.name();
                final String description = Objects.requireNonNullElse(workItemField.getDescription(), "");
                final WorkItemField value = new WorkItemField(referenceName, name, description, typeName, readOnly);
                if (filter.test(value)) {
                    m.put(referenceName, value);
                }
            }
        });
        return m;
    }


    public Map<String, WorkItemTypeLinkToCategory> getTypesToCategories() throws WorkItemException {
        final Map<String, WorkItemTypeLinkToCategory> m = new HashMap<>();
        final Map<String, WorkItemType> workItemTypes = getWorkItemTypes();
        final Map<String, org.azdtasks.core.WorkItemTypeCategory> workItemTypeCategories = getWorkItemTypeCategories();
        final Map<String, WorkItemType> refWorkItemTypes = workItemTypes.values()
                .stream()
                .collect(Collectors.toMap(WorkItemType::refName, workItemType -> workItemType));
        final Collection<org.azdtasks.core.WorkItemTypeCategory> values = workItemTypeCategories.values();
        for (org.azdtasks.core.WorkItemTypeCategory category : values) {
            final Set<String> refNames = category.workTypes();
            for (String ref : refNames) {
                final WorkItemType workItemType = refWorkItemTypes.get(ref);
                if (workItemType != null) {
                    final WorkItemTypeLinkToCategory workItemTypeLinkToCategory = new WorkItemTypeLinkToCategory(workItemType, category);
                    m.put(workItemType.name(), workItemTypeLinkToCategory);
                }
            }
        }
        return m;
    }

    public Map<String, org.azdtasks.core.WorkItemTypeCategory> getWorkItemTypeCategories() throws WorkItemException {
        if (!StringUtils.isEmpty(getProject())) {
            return execute(workItemTypeCategoriesRequestBuilder::list, workItemTypeCategoriesImpl -> {
                final Map<String, org.azdtasks.core.WorkItemTypeCategory> m = new HashMap<>();
                final List<WorkItemTypeCategory> workItemTypeCategories = workItemTypeCategoriesImpl.getWorkItemTypeCategories();
                for (WorkItemTypeCategory workItemTypeCategory : workItemTypeCategories) {
                    final String name = workItemTypeCategory.getName();
                    final String referenceName = workItemTypeCategory.getReferenceName();
                    final WorkItemTypeReference defaultWorkItemType = workItemTypeCategory.getDefaultWorkItemType();
                    final String typeName1 = getRefName(defaultWorkItemType);

                    final List<WorkItemTypeReference> workItemTypesRef = workItemTypeCategory.getWorkItemTypes();
                    final Set<String> names = new HashSet<>();
                    for (WorkItemTypeReference workItemTypeReference : workItemTypesRef) {
                        final String typeName = getRefName(workItemTypeReference);
                        names.add(typeName);
                    }
                    org.azdtasks.core.WorkItemTypeCategory workItemTypeCategory1 = new org.azdtasks.core.WorkItemTypeCategory(name, referenceName, typeName1, names);
                    m.put(workItemTypeCategory.getReferenceName(), workItemTypeCategory1);
                }
                return m;
            });
        } else {
            LOG.warn("Not Project was specified");
            return Map.of();
        }
    }

    private String getRefName(WorkItemTypeReference workItemTypeReference) {
        final String url = workItemTypeReference.getUrl();
        final URI uri = URI.create(url);
        final String path = uri.getPath();
        final Path path1 = Path.of(path);
        final int nameCount = path1.getNameCount();
        final Path name = path1.getName(nameCount - 1);
        return name.toString();
    }


    public Map<String, WorkItemType> getWorkItemTypes() throws WorkItemException {
        String project1 = getProject();
        if (!StringUtils.isEmpty(project1)) {
            return execute(workItemTypesRequestBuilder::list, workItemTypesImpl -> {
                final Map<String, WorkItemType> workItemTypes = new HashMap<>();
                for (org.azd.workitemtracking.types.WorkItemType wt : workItemTypesImpl.getWorkItemTypes()) {
                    final List<WorkItemStateColor> states = wt.getStates();
                    final Map<String, String> m = states.stream()
                            .collect(Collectors.toMap(WorkItemStateColor::getName, WorkItemStateColor::getCategory));
                    final String url = wt.getIcon().getUrl();
                    final WorkItemType workItemTy = new WorkItemType(wt.getName(), wt.getReferenceName(), url, m);
                    workItemTypes.put(workItemTy.name(), workItemTy);
                }
                return workItemTypes;
            });
        } else {
            LOG.warn("No project was specified returning empty list for work item types");
            return Map.of();
        }

    }

    private WorkItem getWorkItemImpl(int id) throws WorkItemException {
        final WorkItem t = execute(() -> workItemsRequestBuilder.get(id, r -> r.queryParameters.expand = WorkItemExpand.ALL), Function.identity());
        final ApiResponse response = t.getResponse();
        final URI requestUri = response.getRequestInformation().getRequestUri();
        final HttpStatusCode statusCode = response.getStatusCode();
        if (!statusCode.equals(HttpStatusCode.OK)) {
            final String formatted = "Code: %d - %s , %s".formatted(statusCode.getCode(), statusCode.getMessage(), response.getResponseBody());
            throw new WorkItemException(statusCode.getCode(), "Problem occurred: " + requestUri + " " + formatted);
        }
        return t;
    }

    public WorkItemModel getWorkItem(int id) throws WorkItemException {
        final List<WorkItemModel> workItems = getWorkItems(id);
        if (workItems.isEmpty()) {
            return null;
        } else {
            return workItems.getFirst();
        }
    }

    //    public void addWorkItemComment(int id, String text) throws WorkItemException {
//        final WorkItemModel workItemModel = updateWorkItem(id, "System.History", text);
//    }
    public void addWorkItemComment(int id, String text) throws WorkItemException {
        execute((AzExec<Comment>) () -> commentsRequestBuilder.add(text, id), Function.identity());
    }

    public WorkItemModel updateWorkItem(int id, String fieldName, String value) throws WorkItemException {
        final WorkItem workItem = updateWorkItem(id, Map.of(fieldName, value));
        return convertToModel(workItem);
    }

    private WorkItem updateWorkItem(int id, Map<String, Object> fields) throws WorkItemException {
        final List<JsonPatchDocument> patchDocuments = new ArrayList<>();
        final JsonPatchDocument d = new JsonPatchDocument();
        for (String s : fields.keySet()) {
            final Object o = fields.get(s);
            d.setOperation(PatchOperation.REPLACE);
            d.setPath("/fields/" + s);
            d.setValue(o);
        }
        patchDocuments.add(d);
        return execute(() -> workItemsRequestBuilder.update(id, patchDocuments), workItem -> workItem);
    }

    public WorkItemModel updateWorkItemState(int id, String state) throws WorkItemException {
        final WorkItemModel workItem = updateWorkItem(id, "System.State", state);
        return workItem;
    }


    protected Date parseDate(String dateStr) {
        if (StringUtils.isEmpty(dateStr)) {
            return null;
        }
        try {
            // Instant.parse uses ISO_INSTANT, which makes the fractional seconds optional,
            // so it handles both "...:05.183Z" and "...:17Z" with one call.
            final Instant parse = Instant.parse(dateStr);
            return Date.from(parse);
        } catch (DateTimeParseException e) {
            LOG.error("Failed to parse date {}", dateStr, e);
            return null;
        }
    }

    public WorkItemComments getComments(WorkItem workItem) {
        final int id = workItem.getId();
        final WorkItemFields fields = workItem.getFields();
        final int systemCommentCount = fields.getSystemCommentCount();

        final WorkItemComments commentListFor;
        if (systemCommentCount > 1) {
            commentListFor = getCommentListFor(id);
        } else if (systemCommentCount == 1) {
            commentListFor = getCommentListFor(id);
        } else {
            commentListFor = new WorkItemComments(workItem.getId(), List.of());
        }
        return commentListFor;
    }

    protected WorkItemModel convertToModel(WorkItem workItem) {
        if (workItem == null) {
            return null;
        }
        final WorkItemComments commentListFor = getComments(workItem);
        return toModel(workItem, commentListFor);
    }

    private WorkItemModel toModel(WorkItem workItem, WorkItemComments commentListFor) {
        final WorkItemFields fields = workItem.getFields();
        final int id = workItem.getId();
        final String title = fields.getSystemTitle();
        final String description = fields.getSystemDescription();
        final String workItemType = fields.getSystemWorkItemType();
        final String state = fields.getSystemState();
        final Author systemAssignedTo = fields.getSystemAssignedTo();
        final String assignedTo = systemAssignedTo != null ? systemAssignedTo.getDisplayName() : "";
        final Map<String, Object> otherFields = fields.getOtherFields();
        final Map<String, String> miscFields = new HashMap<>();
        for (Map.Entry<String, Object> stringObjectEntry : otherFields.entrySet()) {
            final String key = stringObjectEntry.getKey();
            final Object value = stringObjectEntry.getValue();
            miscFields.put(key, value.toString());
        }
        final Object links = workItem.get_links();
        final String url;
        if (links instanceof Map<?, ?> m) {
            final Object html = m.get("html");
            if (html instanceof Map<?, ?> h) {
                final Object href = h.get("href");
                url = (String) href;
            } else {
                url = "";
            }
        } else {
            url = "";
        }

        final Date createdDate = parseDate(fields.getSystemCreatedDate());
        final Date changedDate = parseDate(fields.getSystemChangedDate());


        final WorkItemModel workItemModel = new WorkItemModel(id, title, description, workItemType, state, assignedTo, createdDate, changedDate, commentListFor, url, miscFields);
        return workItemModel;
    }

    public WorkItemComments getCommentListFor(Integer id) {
        try {
            final CompletableFuture<WorkItemComments> t = getCommentsForAsync(id);
            return execute(t);
        } catch (WorkItemException e) {
            return new WorkItemComments(id, List.of());
        }
    }

    private CompletableFuture<WorkItemComments> getCommentsForAsync(int id) throws WorkItemException {
        return wrap(getCommentsFor(id)).thenApply(commentList -> toModelComments(id, commentList));
    }


    protected CompletableFuture<CommentList> getCommentsFor(int id) throws WorkItemException {
        try {
            LOG.debug("Getting comment for {}", id);
            return commentsRequestBuilder.listAsync(id, v -> v.queryParameters.order = CommentSortOrder.asc);
        } catch (AzDException e) {
            LOG.error("Error getting comments for work item %s".formatted(id), e);
            throw new WorkItemException(e);
        }
    }

    private WorkItemComments toModelComments(int workItemId, CommentList commentListResponse) {

        final List<Comment> commentsList = commentListResponse.getComments();
        final List<WorkItemComments.WorkItemComment> list = commentsList.stream()
                .map(v -> {
                    final WorkItemComments.WorkItemComment workItemComment = new WorkItemComments.WorkItemComment(v.getId(), htmlCleaner.clean(v.getText()).getText().toString(), v.getCreatedBy().getDisplayName(), parseDate(v.getCreatedDate()));
                    return workItemComment;
                }).toList();
        return new WorkItemComments(workItemId, list);
    }


    protected List<WorkItemModel> toWorkItemModels(WorkItemList workItemsResult) {
        debug(() -> "Getting Workitem ");
        final List<WorkItem> workItems = workItemsResult.getWorkItems();
        debug(() -> "Mid Workitems ");
        final List<WorkItemModel> list = workItems.parallelStream().map(this::convertToModel).toList();
        debug(() -> "Got WorkItems models");
        return list;
    }

    protected Integer[] toIds(WorkItemQueryResult workItemQueryResult) {
        final Integer[] ids = workItemQueryResult.getWorkItems().stream()
                .map(WorkItemReference::getId)
                .toArray(Integer[]::new);
        return ids;
    }

    public List<WorkItemModel> getWorkItems(Integer... ids) throws WorkItemException {
        if (ids.length == 0) {
            return List.of();
        }
        final List<WorkItem> workItems = execute(() -> workItemsRequestBuilder.list(requestConfiguration -> {
            requestConfiguration.queryParameters.ids = ids;
            requestConfiguration.queryParameters.expand = WorkItemExpand.LINKS;
            requestConfiguration.queryParameters.errorPolicy = WorkItemErrorPolicy.OMIT;
        }), WorkItemList::getWorkItems);

        // Fire every comment fetch in parallel; each pipeline carries its own WorkItem,
        // so there is no shared mutable state and no need to look the item back up by id.
        final List<CompletableFuture<WorkItemModel>> modelFutures = workItems.stream()
                .filter(Objects::nonNull)
                .map(workItem -> {
                    final WorkItemFields fields = workItem.getFields();
                    final int systemCommentCount = fields.getSystemCommentCount();
                    CompletableFuture<WorkItemComments> commentsFuture;
                    if (systemCommentCount > 0) {
                        try {
                            commentsFuture = getCommentsForAsync(workItem.getId());
                        } catch (WorkItemException e) {
                            LOG.error("Error getting workitem {}", workItem.getId(), e);
                            commentsFuture = CompletableFuture.completedFuture(new WorkItemComments(workItem.getId()));
                        }
                    } else {
                        commentsFuture = CompletableFuture.completedFuture(new WorkItemComments(workItem.getId()));
                    }
                    return commentsFuture.thenApply(comments -> toModel(workItem, comments));
                })
                .toList();

        // Wait for all pipelines, then collect results in the original work-item order.
        final CompletableFuture<List<WorkItemModel>> result = CompletableFuture.allOf(modelFutures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> modelFutures.stream()
                        .map(CompletableFuture::join)
                        .toList());

        final List<WorkItemModel> execute = execute(result);
        return execute;
    }

    public List<WorkItemModel> executeQuery(String team, String query) throws WorkItemException {
        LOG.info("Query: {}", query);
        final Integer[] ids = execute(() -> wiql.query(team == null ? "" : team, query, requestConfiguration -> {
            requestConfiguration.queryParameters.top = getTop();
            requestConfiguration.queryParameters.timePrecision = isTimePrecision();
        }), this::toIds);
        final List<WorkItemModel> list = getWorkItems(ids);
        LOG.info("Executed Query results: {}", ids.length);
        return list;
    }


    public Map<String, String> getProjects() throws WorkItemException {
        return execute(projects::list, o -> {
            final List<Project> teamsList = o.getProjects();
            final Map<String, String> teamsMap = teamsList.stream()
                    .collect(Collectors.toMap(Project::getId, Project::getName));
            return teamsMap;
        });
//        final CompletableFuture<Map<String, String>> mapCompletableFuture = wrap(getProjectsImpl()).thenApply(this::toProjects);
//        return execute(mapCompletableFuture);
    }


    private interface AzExec<T> {
        T invoke() throws AzDException;
    }

    private <R, T extends SerializableEntity> R execute(AzExec<T> exec, Function<T, R> function) throws WorkItemException {
        try {
            final T invoke = exec.invoke();
            return function.apply(invoke);
        } catch (AzDException e) {
            LOG.error("Problem", e);
            throw new WorkItemException(e);
        }
    }

    public Map<String, String> getTeams() throws WorkItemException {
//        final CompletableFuture<Map<String, String>> t = wrap(getTeamsImpl()).thenApply(this::toTeams);
//        final Map<String, String> execute = execute(t);
        return execute(teams::list, o -> {
            final List<WebApiTeam> teamsList = o.getTeams();
            final Map<String, String> teamsMap = teamsList.stream()
                    .collect(Collectors.toMap(WebApiTeam::getId, WebApiTeam::getName));
            return teamsMap;
        });
    }


    private <R> R execute(CompletableFuture<R> t) throws WorkItemException {
        try {
            return t.join();
        } catch (CompletionException e) {
            return unWrap(e);
        }
    }

    protected <T extends SerializableEntity> CompletableFuture<T> wrap(CompletableFuture<T> f) {
        return f.exceptionally(throwable -> {
            if (throwable instanceof AzDException azDException) {
                throw new CompletionException(new WorkItemException(azDException));
            } else {
                throw new CompletionException(throwable);
            }
        });
    }


    private <R> R unWrap(CompletionException e) throws WorkItemException {
        final Throwable cause = e.getCause();
        if (cause instanceof WorkItemException ex) {
            throw ex;
        } else {
            throw e;
        }
    }

    protected void debug(Supplier<String> supplier) {
        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug(supplier.get());
            } catch (Throwable e) {
                LOG.error("Error logging ", e);
            }
        }
    }


    private ClientRequest getWorkItemTypeCategoriesBuilder() throws AzDException {
        final AccessTokenCredential accessTokenCredential = azDServiceClient.accessTokenCredential();
        final String projectName = accessTokenCredential.getProjectName();
        final String locationUrl = locationsBaseRequestBuilder.getUrl(ResourceId.WIT);
        final String descriptors = locationsBaseRequestBuilder.getConnectionData().getAuthenticatedUser().getDescriptor();
        // Construct the request URI
        final URI requestUri = UrlBuilder.fromBaseUrl(locationUrl)
                .appendPath(projectName)
                .appendPath(Constants.APIS_RELATIVE_PATH)
                .appendPath("wit")
                .appendPath("workitemtypecategories")
                .appendQueryString(Constants.API_VERSION, ApiVersion.WORK_ITEM_TYPES)
                .appendQueryString("descriptors", descriptors)
                .build();
        final ClientRequest workItemTypeCategoriesRequest = ClientRequest.builder(accessTokenCredential).URI(requestUri).build();
        return workItemTypeCategoriesRequest;
    }
}

