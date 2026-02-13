import org.azd.core.types.Processes;
import org.azd.core.types.Projects;
import org.azd.core.types.WebApiTeams;
import org.azd.exceptions.AzDException;
import org.azd.interfaces.CoreDetails;
import org.azd.interfaces.WorkItemTrackingDetails;
import org.azd.utils.AzDClientApi;
import org.azd.workitemtracking.types.WorkItem;
import org.azd.workitemtracking.types.WorkItemStateColor;
import org.azd.workitemtracking.types.WorkItemType;
import org.azd.workitemtracking.types.WorkItemTypes;
import org.azdtasks.core.client.WorkItemClient;
import org.azdtasks.core.client.WorkItemLegacyClient;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestAPIScratch {


    
    @Test
     @Ignore
    public void test2() throws AzDException {
        String organisation =System.getProperty("o");
   
        String project = System.getProperty("p");
        String personalAccessToken =System.getProperty("t");

       // new WorkItemClient(organisation, project, personalAccessToken).getProjects()
        ///  final Map<String, org.azdtasks.core.WorkItemType> workItemTypes2 = new WorkItemLegacyClient(organisation,project, personalAccessToken).getWorkItemTypes();
        // Connect Azure DevOps API with the organisation name and personal access token.
        final AzDClientApi azDClientApi = new AzDClientApi(organisation, project, personalAccessToken);
        final CoreDetails coreApi = azDClientApi.getCoreApi();
        final Projects projects = coreApi.getProjects();
        System.out.println(projects);
//        final Processes processes = coreApi.getProcesses();
//        final WebApiTeams teams = coreApi.getTeams();
//
//        final WorkItemTrackingDetails workItemTrackingApi = azDClientApi.getWorkItemTrackingApi();
//        final WorkItem workItem = workItemTrackingApi.getWorkItem(16858);
//        final String systemState = workItem.getFields().getSystemState();
//        final WorkItemTypes workItemTypes1 = workItemTrackingApi.getWorkItemTypes();
//
//        final List<WorkItemType> workItemTypes = workItemTypes1.getWorkItemTypes();
//        final Map<String,List<WorkItemStateColor>> workItemTypeStates = new HashMap<>();
//        for (WorkItemType workItemType : workItemTypes) {
//            final List<WorkItemStateColor> states = workItemType.getStates();
//            final String name = workItemType.getName();
//            workItemTypeStates.computeIfAbsent(name, k -> states);
//            System.out.println(workItemType);
//        }
//        workItemTrackingApi.updateWorkItem(16858,Map.of("System.State","New"));
//        System.out.println(workItem);   
    }
}
