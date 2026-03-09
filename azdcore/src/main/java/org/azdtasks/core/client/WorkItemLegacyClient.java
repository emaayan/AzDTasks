package org.azdtasks.core.client;

import org.azd.authentication.PersonalAccessTokenCredential;
import org.azd.core.types.Projects;
import org.azd.core.types.WebApiTeams;
import org.azd.enums.GetFieldsExpand;
import org.azd.enums.WorkItemExpand;
import org.azd.exceptions.AzDException;
//import org.azd.interfaces.AzDClient;
//import org.azd.interfaces.WorkItemTrackingDetails;
//import org.azd.utils.AzDClientApi;
import org.azd.http.ClientRequest;
import org.azd.workitemtracking.types.*;
import org.azdtasks.core.WorkItemComments;
import org.azdtasks.core.WorkItemException;
import org.azdtasks.core.WorkItemModel;
import org.azdtasks.core.types.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class WorkItemLegacyClient extends AbstractWorkItemClient {

    private final static Logger LOG = LoggerFactory.getLogger(WorkItemLegacyClient.class);

//    private final AzDClient azDClient;
//    private final WorkItemTrackingDetails workItemTrackingApi;

    public WorkItemLegacyClient(String organization, String personalAccessToken) {
        super(organization, personalAccessToken);
//        azDClient = new AzDClientApi(organization, personalAccessToken);
//        workItemTrackingApi = azDClient.getWorkItemTrackingApi();
    }

    public WorkItemLegacyClient(String organization, String project, String personalAccessToken) {
        super(organization, project, personalAccessToken);
//        azDClient = new AzDClientApi(organization, project, personalAccessToken);
//        workItemTrackingApi = azDClient.getWorkItemTrackingApi();
    }

    @Override
    protected List<WorkItemField> getWorkItemFieldsImpl(GetFieldsExpand getFieldsExpand) throws AzDException {
        return List.of();
    }

    @Override
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
        final org.azdtasks.core.types.CommentList comments = build.execute(org.azdtasks.core.types.CommentList.class);
        final List<Comment> comments1 = comments.getComments();
        final WorkItemComments workItemComments = getWorkItemComments(comments1);
        return workItemComments;
    }

    @Override
    protected WorkItem getWorkItemImpl(int id) throws AzDException {
        return null;
    }

    @Override
    protected WorkItem updateWorkItem(int id, Map<String, Object> fields) throws AzDException {
        return null;
    }

    @Override
    protected WorkItemTypes getWorkItemTypesImpl() throws AzDException {
        return null;
    }

    @Override
    protected WorkItemList getWorkItems(int[] ids) throws WorkItemException {
        return null;
    }

    @Override
    protected WorkItemQueryResult query(String query, String team) throws WorkItemException {
        return null;
    }

    @Override
    protected Projects getProjectsImpl() throws WorkItemException {
        return null;
    }

    @Override
    protected WebApiTeams getTeamsImpl() throws WorkItemException {
        return null;
    }

//    @Override
//    protected WorkItem getWorkItemImpl(int id) throws AzDException {
//        try {
//            WorkItem workItem = workItemTrackingApi.getWorkItem(id, WorkItemExpand.LINKS);
//            return workItem;
//        } catch (AzDException e) {
//            if (e.getMessage().contains("WorkItemUnauthorizedAccessException")) {// for peformence
//                LOG.warn(e.getMessage());
//                return null;
//            } else {
//                throw e;
//            }
//        }
//    }
//
//    @Override
//    protected WorkItem updateWorkItem(int id, Map<String, Object> fields) throws AzDException {
//        final WorkItem workItem1 = workItemTrackingApi.updateWorkItem(id, fields);
//        return workItem1;
//    }
//
//    @Override
//    protected WorkItemTypes getWorkItemTypesImpl() throws AzDException {
//        return workItemTrackingApi.getWorkItemTypes();
//    }
//
//    @Override
//    protected List<WorkItemField> getWorkItemFieldsImpl(GetFieldsExpand getFieldsExpand) throws AzDException {
//        final WorkItemFieldTypes workItemFields = workItemTrackingApi.getWorkItemFields(getFieldsExpand);
//        final List<WorkItemField> workItemFields1 = workItemFields.getWorkItemFields();
//        return workItemFields1;
//    }
//
//    @Override
//    protected WorkItemList getWorkItems(int[] ids) throws WorkItemException {
//        try {
//            return workItemTrackingApi.getWorkItems(ids, WorkItemExpand.LINKS);
//        } catch (AzDException e) {
//            throw new WorkItemException(e);
//        }
//    }
//
//    @Override
//    protected WorkItemQueryResult query(String query, String team) throws WorkItemException {
//        try {
//            return workItemTrackingApi.queryByWiql(team, query, getTop(), isTimePrecision());
//        } catch (AzDException e) {
//            throw new WorkItemException(e);
//        }
//    }
//
//
//    @Override
//    protected Projects getProjectsImpl() throws WorkItemException {
//        try {
//            return azDClient.getCoreApi().getProjects();
//        } catch (AzDException e) {
//            throw new WorkItemException(e);
//        }
//    }
//
//
//    @Override
//    protected WebApiTeams getTeamsImpl() throws WorkItemException {
//        try {
//            return azDClient.getCoreApi().getTeams();
//        } catch (AzDException e) {
//            throw new WorkItemException(e);
//        }
//    }


}
