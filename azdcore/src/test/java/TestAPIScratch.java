import org.azd.authentication.AccessTokenCredential;
import org.azd.authentication.PersonalAccessTokenCredential;
import org.azd.common.types.JsonPatchDocument;
import org.azd.core.CoreRequestBuilder;
import org.azd.core.types.Processes;
import org.azd.core.types.Projects;
import org.azd.core.types.WebApiTeams;
import org.azd.enums.Instance;
import org.azd.enums.WorkItemExpand;
import org.azd.exceptions.AzDException;
//import org.azd.interfaces.CoreDetails;
//import org.azd.interfaces.WorkItemTrackingDetails;
import org.azd.helpers.workitemtracking.WorkItemTrackingHelpersRequestBuilder;
import org.azd.serviceclient.AzDService;
import org.azd.serviceclient.AzDServiceClient;
//import org.azd.utils.AzDClientApi;
import org.azd.workitemtracking.comments.CommentsRequestBuilder;
import org.azd.workitemtracking.types.*;
import org.azd.workitemtracking.workitems.WorkItemsRequestBuilder;
import org.azdtasks.core.WorkItemField;
import org.azdtasks.core.WorkItemModel;
import org.azdtasks.core.WorkItemType;
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
    public void testBug(){
        final String org_not_exists = "bad_name";
        final String organization = "";
        String token=System.getProperty("t");

        AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization), token)).buildClient().helpers().workItemTracking().workItemTypes();
        try {
            final CoreRequestBuilder builder = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(org_not_exists), token)).buildClient().core();
            System.out.println(builder);
        }catch (Throwable t){
            t.printStackTrace();
        }

        final CoreRequestBuilder builder = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization), token)).buildClient().core();
        System.out.println(builder  );
        try {
            final CoreRequestBuilder builder1 = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(org_not_exists), token)).buildClient().core();
            System.out.println(builder1);// this shouldn't happen, i should get an exception!!
        }catch (Throwable t){
            t.printStackTrace();
        }
    }



    @Test
    @Ignore
    public void test1() throws AzDException {
        String organization =System.getProperty("o");

        String project = System.getProperty("p");
        String personalAccessToken =System.getProperty("t");
    //    personalAccessToken="wrwerwrwrewrwerwerwer";
        AzDServiceClient azDServiceClient = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization),project, personalAccessToken)).buildClient();
        WorkItemClient workItemClient = new WorkItemClient(organization, project, personalAccessToken);
        String s= """
                  SELECT [System.Id], [System.Title], [System.Description], [System.WorkItemType], 
                                [System.State], [System.AssignedTo], [System.CreatedDate], [System.ChangedDate] 
                                FROM WorkItems WHERE [System.TeamProject] = '%s' AND 
                                ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s' ) 
                                ORDER BY [System.ChangedDate] DESC
                """;
        s= """
           SELECT [System.Id]
           FROM WorkItems
           WHERE ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s')
           ORDER BY [System.ChangedDate] DESC
           """;
//        s= """
//                SELECT [System.Id]
//                FROM WorkItems
//                WHERE
//                    [System.AssignedTo] = @Me
//                    AND [System.StateCategory] IN ('Proposed', 'InProgress')
//                ORDER BY [System.CreatedDate] DESC
//                """;
        List<WorkItemModel> workItemModels1 = workItemClient.executeQuery("", s);
        String formatted = s.formatted( "Defect", "Test Defect");
        List<WorkItemModel> workItemModels = workItemClient.executeQuery("", formatted);

        Map<String, WorkItemType> workItemTypes = workItemClient.getWorkItemTypes();
        Map<String, WorkItemField> workItemFields = workItemClient.getWorkItemFields(v -> true);
        WorkItemTrackingHelpersRequestBuilder workItemTrackingHelpersRequestBuilder = azDServiceClient.helpers().workItemTracking();
        CommentsRequestBuilder comments = workItemTrackingHelpersRequestBuilder.comments();
//        comments.add("this is my comment ",26722);
        CommentList list = comments.list(26722);
        List<Comment> comments1 = list.getComments();
        for (Comment comment : comments1) {
            System.out.println(comment);
        }
        System.out.println(list);
        WorkItemsRequestBuilder workItemsRequestBuilder = workItemTrackingHelpersRequestBuilder.workItems();
        WorkItem workItem = workItemsRequestBuilder.get(26722,v->v.queryParameters.expand= WorkItemExpand.ALL);


        System.out.println(workItem);

    }
    @Test
     @Ignore
    public void test2() throws AzDException {
        String organisation =System.getProperty("o");
   
        String project = System.getProperty("p");
        String personalAccessToken =System.getProperty("t");


        WorkItemLegacyClient workItemLegacyClient = new WorkItemLegacyClient(organisation, null, personalAccessToken);
        WorkItemModel workItem1 = workItemLegacyClient.getWorkItem(2);
        Map<String, WorkItemField> workItemFields = workItemLegacyClient.getWorkItemFields(v->v.isSystem() && v.type().equals("DOUBLE"));

        System.out.println(workItemFields);
//        // Connect Azure DevOps API with the organisation name and personal access token.
//        final AzDClientApi azDClientApi = new AzDClientApi(organisation, project, personalAccessToken);
//        WorkItemTrackingDetails workItemTrackingApi = azDClientApi.getWorkItemTrackingApi();

//        final CoreDetails coreApi = azDClientApi.getCoreApi();
//        List<WorkItemField> workItemFields = azDClientApi.getWorkItemTrackingApi().getWorkItemFields().getWorkItemFields();
//
//        final Projects projects = coreApi.getProjects();
//        System.out.println(projects);
//        final Processes processes = coreApi.getProcesses();
//        final WebApiTeams teams = coreApi.getTeams();
//

//        JsonPatchDocument patchDocument = new JsonPatchDocument();
//        patchDocument. = "add";
//        patchDocument.path = "/fields/System.History";
//        patchDocument.value = "Your comment text here";

//        final WorkItemTrackingDetails workItemTrackingApi = azDClientApi.getWorkItemTrackingApi();
        //workItemTrackingApi.addWorkItemAttachment()
        int id = 16876;
//        final WorkItem workItem = workItemTrackingApi.getWorkItem(1);
//        workItemTrackingApi.updateWorkItem(id,Map.of("System.History","this is a test"));
      //  final String systemState = workItem.getFields().getSystemState();
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
