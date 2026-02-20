package org.azdtasks.core.client;

import org.azd.authentication.PersonalAccessTokenCredential;
import org.azd.common.types.Author;
import org.azd.core.types.*;
import org.azd.enums.FieldType;
import org.azd.enums.FieldUsage;
import org.azd.enums.GetFieldsExpand;
import org.azd.enums.Instance;
import org.azd.exceptions.AzDException;
import org.azd.http.ClientRequest;
import org.azd.workitemtracking.types.*;
import org.azdtasks.core.WorkItemComments;
import org.azdtasks.core.WorkItemException;
import org.azdtasks.core.WorkItemField;
import org.azdtasks.core.WorkItemModel;
import org.azdtasks.core.WorkItemType;
import org.azdtasks.core.types.Comment;
import org.azdtasks.core.types.CommentList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractWorkItemClient {

    private final static Logger LOG = LoggerFactory.getLogger(AbstractWorkItemClient.class);

    public static String toURL(String org) {
        return Instance.BASE_INSTANCE.append(org);
    }

    protected final String organization;
    protected final String personalAccessToken;
    protected final String project;


    public AbstractWorkItemClient(String organization, String personalAccessToken) {
        this(organization, null, personalAccessToken);
    }

    public AbstractWorkItemClient(String organization, String project, String personalAccessToken) {
        this.organization = organization;
        this.personalAccessToken = personalAccessToken;
        this.project = project;
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


    public Map<String, WorkItemField> getWorkItemFields(Predicate<WorkItemField> filter) throws WorkItemException {
        try {
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
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    protected abstract List<org.azd.workitemtracking.types.WorkItemField> getWorkItemFieldsImpl(GetFieldsExpand getFieldsExpand) throws AzDException;

    public Map<String, WorkItemType> getWorkItemTypes() throws WorkItemException {
        try {
            final org.azd.workitemtracking.types.WorkItemTypes workItemTypesImpl = getWorkItemTypesImpl();
            final Map<String, WorkItemType> workItemTypes = new HashMap<>();
            for (org.azd.workitemtracking.types.WorkItemType wt : workItemTypesImpl.getWorkItemTypes()) {
                final List<WorkItemStateColor> states = wt.getStates();
                final Map<String, String> m = states.stream()
                        .collect(Collectors.toMap(WorkItemStateColor::getName, WorkItemStateColor::getCategory));
                final WorkItemType workItemTy = new WorkItemType(wt.getName(), m);
                workItemTypes.put(workItemTy.name(), workItemTy);
            }
            return workItemTypes;
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    public WorkItemModel getWorkItem(int id) throws WorkItemException {
        try {
            final WorkItem workItemImpl = getWorkItemImpl(id);
            if (workItemImpl != null) {
                final WorkItemModel workItemModel = convertToModel(workItemImpl);
                return workItemModel;
            } else {
                return null;
            }
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    protected abstract WorkItem getWorkItemImpl(int id) throws AzDException;

    public WorkItemModel updateWorkItemState(int id, String state) throws WorkItemException {
        final WorkItemModel workItem = updateWorkItem(id, "System.State", state);
        return workItem;
    }

    public WorkItemModel addWorkItemComment(int id, String text) throws WorkItemException {
        final WorkItemModel workItemModel = updateWorkItem(id, "System.History", text);
        return workItemModel;
    }

    public WorkItemModel updateWorkItem(int id, String fieldName, String value) throws WorkItemException {
        try {
            final WorkItem workItem = updateWorkItem(id, Map.of(fieldName, value));
            return convertToModel(workItem);
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    protected abstract WorkItem updateWorkItem(int id, Map<String, Object> fields) throws AzDException;

    protected abstract org.azd.workitemtracking.types.WorkItemTypes getWorkItemTypesImpl() throws AzDException;

    protected Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(dateStr);
        } catch (ParseException e) {
            LOG.error("Failed to parse date {}", dateStr, e);
            return null;
        }
    }

    protected WorkItemModel convertToModel(WorkItem workItem) {
        if (workItem == null) {
            return null;
        }

        final WorkItemFields fields = workItem.getFields();
        final int id = workItem.getId();
        final String title = fields.getSystemTitle();
        final String description = fields.getSystemDescription();
        final String workItemType = fields.getSystemWorkItemType();
        final String state = fields.getSystemState();
        final Author systemAssignedTo = fields.getSystemAssignedTo();
        final String assignedTo = systemAssignedTo != null ? systemAssignedTo.getDisplayName() : "";
        final Map<String, Object> otherFields = fields.getOtherFields();

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
        final int systemCommentCount = fields.getSystemCommentCount();

        final WorkItemComments commentListFor;
        if (systemCommentCount > 1) {
            commentListFor = getCommentListFor(id);
        } else if (systemCommentCount == 1) {
//            if (otherFields != null) {
//                final Object history = otherFields.getOrDefault("System.History", "");
//            }
            commentListFor = getCommentListFor(id);
        } else {
            commentListFor = new WorkItemComments(List.of());
        }

        final WorkItemModel workItemModel = new WorkItemModel(id, title, description, workItemType, state, assignedTo, createdDate, changedDate, commentListFor, url);
        return workItemModel;
    }

    public WorkItemComments getCommentListFor(Integer id) {
        try {
            final WorkItemComments commentsFor = getCommentsFor(id);
            return commentsFor;
        } catch (AzDException e) {
            LOG.error("Error getting comments for work item {}", id, e);
            return new WorkItemComments(List.of());
        }
    }

    protected WorkItemComments getCommentsFor(int id) throws AzDException {
        final String url = toURL(getOrganization());
        final PersonalAccessTokenCredential accessTokenCredential = new PersonalAccessTokenCredential(url, getProject(), getPersonalAccessToken());
        final ClientRequest build = ClientRequest.builder(accessTokenCredential)
                .baseInstance(accessTokenCredential.getOrganizationUrl())
                .location("608aac0a-32e1-4493-a863-b9cf4566d257")
                .area("wit")
                .apiVersion("7.2-preview.4")
                .serviceEndpoint("workItemId", id)
                .build();
        final CommentList comments = build.execute(CommentList.class);
        final List<Comment> comments1 = comments.getComments();
        final List<WorkItemComments.WorkItemComment> list = comments1.stream().map(v -> new WorkItemComments.WorkItemComment(v.getId(), v.getText(), v.getCreatedBy().getDisplayName(), v.getCreatedDate())).toList();
        final WorkItemComments workItemComments = new WorkItemComments(list);
        return workItemComments;
    }


    protected String toQuery(String searchText) {
        final String escapedSearch = searchText.replace("'", "''");
        final String query = """
                SELECT [System.Id], [System.Title], [System.Description], [System.WorkItemType], \
                [System.State], [System.AssignedTo], [System.CreatedDate], [System.ChangedDate] \
                FROM WorkItems WHERE [System.TeamProject] = '%s' AND \
                ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s' ) \
                ORDER BY [System.ChangedDate] DESC""".formatted(project, escapedSearch, escapedSearch);
        return query;
    }

    protected List<WorkItemModel> toWorkItemModels(WorkItemList workItemsList) {
        final List<WorkItem> workItems = workItemsList.getWorkItems();
        final List<WorkItemModel> list = workItems.parallelStream().map(this::convertToModel).toList();
        return list;
    }

    protected int[] toIds(WorkItemQueryResult workItemQueryResult) {
        final List<WorkItemReference> workItemRef = workItemQueryResult.getWorkItems();
        final int[] ids = workItemRef.stream().mapToInt(WorkItemReference::getId).toArray();
        return ids;
    }

    public List<WorkItemModel> searchWorkItems(String string, String team) throws WorkItemException {
        final String query = toQuery(string);
        LOG.info("Query: {}", query);
        try {
            final WorkItemQueryResult workItemQueryResult = query(query, team);
            final int[] ids = toIds(workItemQueryResult);
            if (ids.length > 0) {
                final WorkItemList workItemsList = getWorkItems(ids);
                final List<WorkItemModel> list = toWorkItemModels(workItemsList);
                return list;
            } else {
                return List.of();
            }
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    protected Map<String, String> toProjects(final Projects projects) {
        final List<Project> projects1 = projects.getProjects();
        final Map<String, String> projectsList = projects1.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        return projectsList;
    }

    protected Map<String, String> toTeams(final WebApiTeams teams) {
        final List<WebApiTeam> teamsList = teams.getTeams();
        final Map<String, String> projectsList = teamsList.stream()
                .collect(Collectors.toMap(WebApiTeam::getId, WebApiTeam::getName));
        return projectsList;
    }


    public Map<String, String> getProjects() throws WorkItemException {
        try {
            final Projects projects = getProjectsImpl();
            final Map<String, String> projectList = toProjects(projects);
            return projectList;
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    public Map<String, String> getTeams() throws WorkItemException {
        try {
            final WebApiTeams teams = getTeamsImpl();
            final Map<String, String> teamList = toTeams(teams);
            return teamList;
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    protected abstract WorkItemList getWorkItems(int[] ids) throws WorkItemException;

    protected abstract WorkItemQueryResult query(String query, String team) throws WorkItemException;

    protected abstract Projects getProjectsImpl() throws WorkItemException;

    protected abstract WebApiTeams getTeamsImpl() throws WorkItemException;


}

