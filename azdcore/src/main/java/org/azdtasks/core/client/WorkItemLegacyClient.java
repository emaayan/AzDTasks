package org.azdtasks.core.client;

import org.azd.core.types.Projects;
import org.azd.core.types.WebApiTeams;
import org.azd.enums.WorkItemExpand;
import org.azd.exceptions.AzDException;
import org.azd.interfaces.AzDClient;
import org.azd.interfaces.WorkItemTrackingDetails;
import org.azd.utils.AzDClientApi;
import org.azd.workitemtracking.types.WorkItem;
import org.azd.workitemtracking.types.WorkItemList;
import org.azd.workitemtracking.types.WorkItemQueryResult;
import org.azd.workitemtracking.types.WorkItemTypes;
import org.azdtasks.core.WorkItemModel;

import java.util.Map;

public class WorkItemLegacyClient extends AbstractWorkItemClient {

    private final AzDClient azDClient;
    private final WorkItemTrackingDetails workItemTrackingApi;

    public WorkItemLegacyClient(String organization, String personalAccessToken) {
        super(organization, personalAccessToken);
        azDClient = new AzDClientApi(organization, personalAccessToken);
        workItemTrackingApi = azDClient.getWorkItemTrackingApi();
    }

    public WorkItemLegacyClient(String organization, String project, String personalAccessToken) {
        super(organization, project, personalAccessToken);
        azDClient = new AzDClientApi(organization, project, personalAccessToken);
        workItemTrackingApi = azDClient.getWorkItemTrackingApi();
    }

    @Override
    protected WorkItem updateWorkItem(int id, Map<String, Object> fields) throws AzDException {
        final WorkItem workItem1 = workItemTrackingApi.updateWorkItem(id, fields);
        return workItem1;
    }

    @Override
    protected WorkItemTypes getWorkItemTypesImpl() throws AzDException {
        return  workItemTrackingApi.getWorkItemTypes();
    }

    @Override
    protected WorkItemList getWorkItems(int[] ids) throws AzDException {
        return workItemTrackingApi.getWorkItems(ids, WorkItemExpand.LINKS);
    }

    @Override
    protected WorkItemQueryResult query(String query, String team) throws AzDException {
        return workItemTrackingApi.queryByWiql(team, query, getTop(), isTimePrecision());
    }

    
    @Override
    protected Projects getProjectsImpl() throws AzDException {
        return azDClient.getCoreApi().getProjects();
    }


    @Override
    protected WebApiTeams getTeamsImpl() throws AzDException {
        return azDClient.getCoreApi().getTeams();
    }

    @Override
    public WorkItemModel getWorkItem(int id) throws AzDException {
        final WorkItem workItem = workItemTrackingApi.getWorkItem(id);
        final WorkItemModel workItemModel = convertToModel(workItem);
        return workItemModel;
    }

}
