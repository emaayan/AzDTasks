package org.azdtasks.core.client;


import org.azd.abstractions.ApiResponse;
import org.azd.abstractions.serializer.SerializableEntity;
import org.azd.authentication.PersonalAccessTokenCredential;
import org.azd.common.types.JsonPatchDocument;
import org.azd.core.types.*;
import org.azd.enums.*;
import org.azd.exceptions.AzDException;
import org.azd.serviceclient.AzDService;
import org.azd.serviceclient.AzDServiceClient;
import org.azd.workitemtracking.WorkItemTrackingRequestBuilder;
import org.azd.workitemtracking.comments.CommentsRequestBuilder;
import org.azd.workitemtracking.fields.FieldsRequestBuilder;
import org.azd.workitemtracking.types.*;
import org.azd.workitemtracking.types.WorkItemField;
import org.azd.workitemtracking.types.WorkItemTypes;
import org.azd.workitemtracking.workitems.WorkItemsRequestBuilder;
import org.azdtasks.core.*;

import java.net.URI;
import java.util.*;

public class WorkItemClient extends AbstractWorkItemClient {

    private static <T extends SerializableEntity> T check(T t) throws WorkItemException {
        final ApiResponse response = t.getResponse();
        final URI requestUri = response.getRequestInformation().getRequestUri();

        final HttpStatusCode statusCode = response.getStatusCode();
        if (!statusCode.equals(HttpStatusCode.OK)) {
            final String formatted = "Code: %d - %s , %s".formatted(statusCode.getCode(), statusCode.getMessage(), response.getResponseBody());
            throw new WorkItemException(statusCode.getCode(), "Problem occurred: " + requestUri + " " + formatted);
        }
        return t;
    }


    private static AzDServiceClient getClient(String org, String project, String personalAccessToken) {
        final PersonalAccessTokenCredential pat = new PersonalAccessTokenCredential(toURL(org), project, personalAccessToken);
        final AzDServiceClient service = AzDService.builder().authentication(pat).buildClient();
        return service;
    }

    private final AzDServiceClient azDServiceClient;
    private final WorkItemTrackingRequestBuilder wit;
    private final CommentsRequestBuilder commentsRequestBuilder;

    public WorkItemClient(String organization, String project, String personalAccessToken) {
        super(organization, project, personalAccessToken);
        azDServiceClient = getClient(organization, project, personalAccessToken);
        wit = azDServiceClient.workItemTracking();
        commentsRequestBuilder = wit.comments();
    }


    @Override
    protected WorkItem getWorkItemImpl(int id) throws AzDException {
        final WorkItemsRequestBuilder workItemsRequestBuilder = wit.workItems();
        return check(workItemsRequestBuilder.get(id, r -> r.queryParameters.expand = WorkItemExpand.ALL));
    }

    @Override
    protected WorkItemComments getCommentsFor(int id) throws AzDException {
        List<Comment> commentsList = commentsRequestBuilder.list(id).getComments();
        List<WorkItemComments.WorkItemComment> list = commentsList.stream().map(v -> new WorkItemComments.WorkItemComment(v.getId(), v.getText(), v.getCreatedBy().getDisplayName(),parseDate(v.getCreatedDate()))).toList();
        return new WorkItemComments(list);
    }

    @Override
    public void addWorkItemComment(int id, String text) throws WorkItemException {
        try {
            final Comment add = commentsRequestBuilder.add( text,id);
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    @Override
    protected WorkItem updateWorkItem(int id, Map<String, Object> fields) throws AzDException {
        final List<JsonPatchDocument> patchDocuments = new ArrayList<>();
        final JsonPatchDocument d = new JsonPatchDocument();
        for (String s : fields.keySet()) {
            final Object o = fields.get(s);
            d.setOperation(PatchOperation.REPLACE);
            d.setPath("/fields/" + s);
            d.setValue(o);
        }
        patchDocuments.add(d);
        final WorkItem update = wit.workItems().update(id, patchDocuments);
        return update;
    }

    @Override
    protected WorkItemTypes getWorkItemTypesImpl() throws AzDException {
        return wit.workItemTypes().list();
    }

    private String get(Map m,String name,String defValue){
        Object orDefault = m.getOrDefault(name, defValue);
        Object o = Objects.requireNonNullElse(orDefault, defValue);
        return o.toString();
    }

    @Override
    protected List<WorkItemField> getWorkItemFieldsImpl(GetFieldsExpand getFieldsExpand) throws AzDException {
        final FieldsRequestBuilder fieldsRequestBuilder = wit.fields();
        final List<WorkItemField> fields=new ArrayList<>();
        final Map<String, Object> otherFields = fieldsRequestBuilder.list(getFieldsExpand).getOtherFields();
        final List<Object> values =  (List<Object>) otherFields.getOrDefault("value",List.of());
        for (Object value : values) {
            if (value instanceof Map m){
                final String id = get(m,"referenceName","");
                final String name = get(m,"name","");
                final String description = get(m,"description","");
                final String type = get(m,"type","");
                final String usage = get(m, "usage", "");
                final boolean readOnly =Boolean.parseBoolean(get(m,"readOnly",Boolean.FALSE.toString()));
                WorkItemField workItemField1 = new WorkItemField();
                workItemField1.setReferenceName(id);
                workItemField1.setDescription(description);
                workItemField1.setName(name);
                FieldUsage fieldUsage = FieldUsage.valueOf(usage.toUpperCase());
                workItemField1.setUsage(fieldUsage);
                FieldType fieldType = FieldType.valueOf(type.toUpperCase());
                workItemField1.setType(fieldType);
                workItemField1.setReadOnly(readOnly);
                fields.add(workItemField1);
            }
        }
        return fields;
       // return values.stream().map(o -> (WorkItemField) o).toList();
    }

    @Override
    protected WorkItemList getWorkItems(int[] ids) throws WorkItemException {
        final WorkItemsRequestBuilder workItemsRequestBuilder = wit.workItems();
        try {
            return check(workItemsRequestBuilder.list(requestConfiguration -> {
                requestConfiguration.queryParameters.ids = Arrays.stream(ids).boxed().toArray(Integer[]::new);
                requestConfiguration.queryParameters.expand = WorkItemExpand.LINKS;
            }));
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }


    @Override
    protected WorkItemQueryResult query(String q, String team) throws WorkItemException {
        final WorkItemQueryResult workItemQueryResult;
        try {
            workItemQueryResult = wit.wiql().query(team, q, requestConfiguration -> {
                requestConfiguration.queryParameters.top = getTop();
                requestConfiguration.queryParameters.timePrecision = isTimePrecision();
            });
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
        return workItemQueryResult;
    }

    @Override
    protected Projects getProjectsImpl() throws WorkItemException {
        try {
            return azDServiceClient.core().projects().list();
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

    @Override
    protected WebApiTeams getTeamsImpl() throws WorkItemException {
        try {
            return azDServiceClient.core().teams().list();
        } catch (AzDException e) {
            throw new WorkItemException(e);
        }
    }

}
