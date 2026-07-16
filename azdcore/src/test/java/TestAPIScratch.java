import org.azd.abstractions.ApiResponse;
import org.azd.abstractions.ResponseHandler;
import org.azd.authentication.AccessTokenCredential;
import org.azd.authentication.PersonalAccessTokenCredential;
import org.azd.common.ApiVersion;
import org.azd.common.Constants;
import org.azd.common.ResourceId;
import org.azd.core.CoreRequestBuilder;
import org.azd.enums.HttpStatusCode;
import org.azd.enums.Instance;
import org.azd.enums.WorkItemErrorPolicy;
import org.azd.exceptions.AzDException;
//import org.azd.interfaces.CoreDetails;
//import org.azd.interfaces.WorkItemTrackingDetails;
import org.azd.helpers.HelpersRequestBuilder;
import org.azd.helpers.workitemtracking.WorkItemTrackingHelpersRequestBuilder;
import org.azd.http.ClientRequest;
import org.azd.locations.LocationsBaseRequestBuilder;
import org.azd.locations.types.ConnectionData;
import org.azd.serviceclient.AzDService;
import org.azd.serviceclient.AzDServiceClient;
//import org.azd.utils.AzDClientApi;
import org.azd.utils.UrlBuilder;
import org.azd.workitemtracking.WorkItemTrackingRequestBuilder;
import org.azd.workitemtracking.comments.CommentsRequestBuilder;
import org.azd.workitemtracking.types.*;
import org.azd.workitemtracking.types.WorkItemTypeCategory;
import org.azd.workitemtracking.wiql.WiqlRequestBuilder;
import org.azd.workitemtracking.workitems.WorkItemsRequestBuilder;
import org.azd.workitemtracking.workitemtypes.WorkItemTypesRequestBuilder;
import org.azdtasks.core.*;
import org.azdtasks.core.WorkItemField;
import org.azdtasks.core.WorkItemType;
import org.azdtasks.core.client.WorkItemClient;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.azd.enums.WorkItemExpand.ALL;

public class TestAPIScratch {


    @Test
    @Ignore
    public void testBug() {
        final String org_not_exists = "bad_name";
        final String organization = "";
        String token = System.getProperty("t");

        AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization), token)).buildClient().helpers().workItemTracking().workItemTypes();
        try {
            final CoreRequestBuilder builder = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(org_not_exists), token)).buildClient().core();
            System.out.println(builder);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        final CoreRequestBuilder builder = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization), token)).buildClient().core();
        System.out.println(builder);
        try {
            final CoreRequestBuilder builder1 = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(org_not_exists), token)).buildClient().core();
            System.out.println(builder1);// this shouldn't happen, i should get an exception!!
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    @Test
    @Ignore
    public void testNative() throws Exception {
        //ClientRequest.Builder wit1 = ClientRequest.builder(accessTokenCredential).baseInstance(accessTokenCredential.getOrganizationUrl()).location("608aac0a-32e1-4493-a863-b9cf4566d257").area("wit").apiVersion("7.2-preview.4");
        //ClientRequest.Builder workItemId = wit1.serviceEndpoint("workItemId", id);
        //    ClientRequest build = workItemId.build();
        //    CommentList execute = build.execute(CommentList.class);
        String organization = System.getProperty("o");
        String project = System.getProperty("p");
        String personalAccessToken = System.getProperty("t");
        PersonalAccessTokenCredential accessTokenCredential = new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization), project, personalAccessToken);
        AzDServiceClient azDServiceClient = AzDService.builder().authentication(accessTokenCredential).buildClient();
        String ha = "HA";
        String s = """
                SELECT [System.Id] 
                FROM WorkItems
                WHERE   ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s' ) 
                ORDER BY [System.ChangedDate] DESC
                """.formatted(ha, ha);
        final WorkItemTrackingRequestBuilder wit = azDServiceClient.workItemTracking();
        final WorkItemsRequestBuilder workItemsRequestBuilder = wit.workItems();
        final WiqlRequestBuilder wiql = wit.wiql();
        final List<WorkItemReference> workItemsRefs = wiql.query("", s, (Consumer<WiqlRequestBuilder.RequestConfiguration>) requestConfiguration -> requestConfiguration.queryParameters.top = 100).getWorkItems();

        final CommentsRequestBuilder commentsBuilder = wit.comments();
        final Integer[] ids = workItemsRefs.stream().map(WorkItemReference::getId).toArray(Integer[]::new);
        Map<Integer, CommentList> m = new HashMap<>();
        final List<WorkItem> workItems = workItemsRequestBuilder.list((Consumer<WorkItemsRequestBuilder.RequestConfiguration>) requestConfiguration -> {
            requestConfiguration.queryParameters.errorPolicy = WorkItemErrorPolicy.OMIT;
            requestConfiguration.queryParameters.ids = ids;
        }).getWorkItems();

        final Map<Integer, CompletableFuture<CommentList>> a = new HashMap<>();
        for (WorkItem workItem : workItems) {
            if (workItem.getFields().getSystemCommentCount() > 0) {
                CompletableFuture<CommentList> commentListCompletableFuture = commentsBuilder.listAsync(workItem.getId());
                a.put(workItem.getId(), commentListCompletableFuture);
            }
            //workItem.getFields().getSystemCommentCount()
        }


//        for (Integer id : ids) {
//
//        }
        Void join = CompletableFuture.allOf(a.values().toArray(CompletableFuture[]::new)).join();
        Set<Map.Entry<Integer, CompletableFuture<CommentList>>> entries = a.entrySet();
        for (Map.Entry<Integer, CompletableFuture<CommentList>> entry : entries) {
            Integer key = entry.getKey();
            CompletableFuture<CommentList> value = entry.getValue();
            m.put(key, value.join());
        }

    }

    @Test
    @Ignore
    public void testSearch() throws Exception {
        String organization = System.getProperty("o");

        String project = System.getProperty("p");
        String personalAccessToken = System.getProperty("t");

//organization="";

//        AzDServiceClient azDServiceClient = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization),  personalAccessToken)).buildClient();
//        try {
//            Function<Throwable, WebApiTeams> throwableWebApiTeamsFunction = throwable -> {
//                if (throwable instanceof AzDException ex) {
//                    throw new CompletionException(new WorkItemException(ex));
//                } else {
//                    throw new CompletionException(throwable);
//                }
//
//            };
//            WebApiTeams join = azDServiceClient.core().teams().listAsync().join();
//        }catch (Throwable t){
//t.printStackTrace();
//        }
//        WorkItemTrackingRequestBuilder workItemTrackingRequestBuilder = azDServiceClient.workItemTracking();
//        WorkItemTypesRequestBuilder workItemTypesRequestBuilder = workItemTrackingRequestBuilder.workItemTypes();
//        try {
//            WorkItemTypes list = workItemTypesRequestBuilder.list();
//        }catch (AzDException e){
//            e.printStackTrace();
//            HttpStatusCode statusCode = ResponseHandler.getResponse().getStatusCode();
//            System.out.println(statusCode);
//        }

        AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization+"seds"), project, personalAccessToken)).buildClient().core();
        AzDServiceClient azDServiceClient =AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization+"seds"), project, personalAccessToken)).buildClient();
        final ConnectionData connectionData = azDServiceClient.locations().getConnectionData();
        LocationsBaseRequestBuilder locations = azDServiceClient.locations();
        System.out.println(locations);
//        WorkItemTrackingRequestBuilder workItemTrackingRequestBuilder = azDServiceClient.workItemTracking();
//        WorkItemTypesRequestBuilder workItemTypesRequestBuilder = workItemTrackingRequestBuilder.workItemTypes();

        //try {
//            CommentsRequestBuilder comments = workItemTrackingRequestBuilder.comments();
            //  CommentList list1 = comments.list(32369);
            //WorkItemTypes list = workItemTypesRequestBuilder.list();

//            String ha = "HA";
//            String s = """
//                    SELECT [System.Id]
//                    FROM WorkItems
//                    WHERE  ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s' )
//                    ORDER BY [System.ChangedDate] DESC
//                    """.formatted(ha, ha);
//            final WorkItemTrackingRequestBuilder wit = azDServiceClient.workItemTracking();
//            final WorkItemsRequestBuilder workItemsRequestBuilder = wit.workItems();
//            final WiqlRequestBuilder wiql = wit.wiql();
//            final List<WorkItemReference> workItemsRefs = wiql.query("", s).getWorkItems();
//            final CommentsRequestBuilder commentsBuilder = wit.comments();
//            final Integer[] ids = workItemsRefs.stream().map(WorkItemReference::getId).toArray(Integer[]::new);
//            final Map<Integer, CommentList> m = new HashMap<>();
//
//            for (Integer id : ids) {
//          //      WorkItem workItem = workItemsRequestBuilder.get(id);
//                CompletableFuture<CommentList> commentListCompletableFuture = commentsBuilder.listAsync(id);
//
//                final CommentList list = commentsBuilder.list(id);
//                m.put(id, list);
//            }
//            final List<WorkItem> workItems = workItemsRequestBuilder.list((Consumer<WorkItemsRequestBuilder.RequestConfiguration>) requestConfiguration -> {
//                requestConfiguration.queryParameters.errorPolicy = WorkItemErrorPolicy.OMIT;
//                requestConfiguration.queryParameters.ids = ids;
//            }).getWorkItems();
//            System.out.println(workItems);
//
//        } catch (AzDException e) {
//            e.printStackTrace();
//            HttpStatusCode statusCode = ResponseHandler.getResponse().getStatusCode();
//            System.out.println(statusCode);
//        }
//        String project1 = workItemClient1.getProject();
//        Map<String, String> teams = workItemClient1.getTeams();
        //   Map<String, WorkItemType> workItemTypes = workItemClient1.getWorkItemTypes();
//        Map<String, WorkItemField> workItemFields = workItemClient1.getWorkItemFields(v -> true);
        Map<String, String> projects = new WorkItemClient(organization, "", personalAccessToken).getProjects();
        Map<String, String> teams = new WorkItemClient(organization, "", personalAccessToken).getTeams();
        Map<String, WorkItemField> workItemFields = new WorkItemClient(organization, "", personalAccessToken).getWorkItemFields(v -> v.isSystem() && v.type().equals("DOUBLE") && !v.isReadOnly());
        WorkItemClient workItemClient = new WorkItemClient(organization, project, personalAccessToken);

        //  Map<String, org.azdtasks.core.WorkItemTypeCategory> workItemTypeCategories = workItemClient.getWorkItemTypeCategories();
        ///Map<String, WorkItemTypeLinkToCategory> typesToCategories = workItemClient.getTypesToCategories();
        workItemClient.setTop(100);
        String ha = "HA";
        String s = """
                SELECT [System.Id] 
                FROM WorkItems
                WHERE  ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s' ) 
                ORDER BY [System.ChangedDate] DESC
                """.formatted(ha, ha);
        //List<WorkItemModel> workItemModels = workItemClient.executeQuery(null, s);
        String ss= """
                SELECT [System.Id]
                FROM WorkItems
                 WHERE System.IterationPath=@CurrentIteration AND [System.AssignedTo] = @Me\s
                ORDER BY Microsoft.VSTS.Common.Priority, System.State                                
                """;

        List<WorkItemModel> workItemModels = workItemClient.executeQuery(null, ss);
        System.out.println(workItemModels.size());
    }

    @Test
    @Ignore
    public void testB() throws Exception {
        String organization = System.getProperty("o");
        String project = System.getProperty("p");
        String personalAccessToken = System.getProperty("t");
        try {
            AzDServiceClient azDServiceClient = AzDService.builder().authentication(new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization), project, personalAccessToken)).buildClient();
            WorkItemTrackingRequestBuilder workItemTrackingRequestBuilder = azDServiceClient.workItemTracking();
            WorkItemsRequestBuilder workItemsRequestBuilder = workItemTrackingRequestBuilder.workItems();
            WiqlRequestBuilder wiql = workItemTrackingRequestBuilder.wiql();
            String s = """
                      SELECT [System.Id], [System.Title], [System.Description], [System.WorkItemType],
                                    [System.State], [System.AssignedTo], [System.CreatedDate], [System.ChangedDate] 
                                    FROM WorkItems WHERE [System.TeamProject] = '%s' AND 
                                    ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s' ) 
                                    ORDER BY [System.ChangedDate] DESC
                    """.formatted("SHS", "Test", "Test");
            WorkItemQueryResult shs = wiql.query("Engine Team", s, v -> v.queryParameters.top = 100);
            List<WorkItemReference> workItems = shs.getWorkItems();
            final Integer[] a = workItems.parallelStream().mapToInt(WorkItemReference::getId).boxed().toArray(Integer[]::new);
            WorkItemList list = workItemsRequestBuilder.list(requestConfiguration -> {
                requestConfiguration.queryParameters.ids = a;
                requestConfiguration.queryParameters.expand = ALL;
            });

            WorkItem workItem = workItemsRequestBuilder.get(1);
//            WorkItemTrackingHelpersRequestBuilder workItemTrackingHelpersRequestBuilder = azDServiceClient.helpers().workItemTracking();
//            final CommentList list = workItemTrackingHelpersRequestBuilder.comments().list(1);
        } catch (AzDException e) {
            e.printStackTrace();
            ApiResponse response = ResponseHandler.getResponse();
            HttpStatusCode statusCode = response.getStatusCode();
            System.out.println(statusCode);
            String requestUrl = response.getRequestUrl();
            System.out.println(requestUrl);
        }

    }


    @Test
    @Ignore
    public void test1() throws Exception {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Test");

        String organization = System.getProperty("o");

        String project = System.getProperty("p");
        String personalAccessToken = System.getProperty("t");

        //    personalAccessToken="wrwerwrwrewrwerwerwer";
        PersonalAccessTokenCredential accessTokenCredential1 = new PersonalAccessTokenCredential(Instance.BASE_INSTANCE.append(organization), project, personalAccessToken);
        AzDServiceClient azDServiceClient = AzDService.builder().authentication(accessTokenCredential1).buildClient();
        HelpersRequestBuilder helpers = azDServiceClient.helpers();

        WorkItemTypesRequestBuilder workItemTypesRequestBuilder = helpers.workItemTracking().workItemTypes();
        WorkItem workItem1 = azDServiceClient.workItemTracking().workItems().get(26722);


        WorkItemClient workItemClient = new WorkItemClient(organization, project, personalAccessToken);

        Map<String, WorkItemTypeLinkToCategory> typesToCategories = workItemClient.getTypesToCategories();
        String systemWorkItemType = workItem1.getFields().getSystemWorkItemType();
        WorkItemTypeLinkToCategory workItemTypeLinkToCategory = typesToCategories.get(systemWorkItemType);
        //Map<String, WorkItemTypeCategory> workItemTypeCategories1 = workItemClient.getWorkItemTypeCategories();
        Map<String, WorkItemType> workItemTypes2 = workItemClient.getWorkItemTypes();
//        Map<String, WorkItemTypeCategory> workItemTypeCategories2 = workItemClient.getWorkItemTypeCategories();
        WorkItemTypes list1 = workItemTypesRequestBuilder.list();

        // Call Azure DevOps REST API.
        List<org.azd.workitemtracking.types.WorkItemType> workItemTypes1 = list1.getWorkItemTypes();
        for (org.azd.workitemtracking.types.WorkItemType workItemType : workItemTypes1) {
            System.out.println(workItemType);
        }

        final String locationUrl = azDServiceClient.locations().getUrl(ResourceId.WIT);
        final String descriptors = azDServiceClient.locations().getConnectionData().getAuthenticatedUser().getDescriptor();
        final AccessTokenCredential accessTokenCredential = azDServiceClient.accessTokenCredential();
        final String projectName = accessTokenCredential.getProjectName();
        // Construct the request URI
        final URI requestUri = UrlBuilder.fromBaseUrl(locationUrl)
                .appendPath(projectName)
                .appendPath(Constants.APIS_RELATIVE_PATH)
                .appendPath("wit")
                .appendPath("workitemtypecategories")
                .appendQueryString(Constants.API_VERSION, ApiVersion.WORK_ITEM_TYPES)
                .appendQueryString("descriptors", descriptors)
                .build();


        WorkItemTypeCategories execute = ClientRequest.builder(accessTokenCredential).URI(requestUri).build().execute(WorkItemTypeCategories.class);// Or use Async method
        List<WorkItemTypeCategory> workItemTypeCategories = execute.getWorkItemTypeCategories();
        for (WorkItemTypeCategory workItemTypeCategory : workItemTypeCategories) {
            System.out.println(workItemTypeCategory);
            List<WorkItemTypeReference> workItemTypes = workItemTypeCategory.getWorkItemTypes();
            for (WorkItemTypeReference workItemTypeReference : workItemTypes) {
                String url = workItemTypeReference.getUrl();
                URI uri = URI.create(url);
                String path = uri.getPath();
                Path path1 = Path.of(path);
                int nameCount = path1.getNameCount();
                Path name = path1.getName(nameCount - 1);
                System.out.println(nameCount);
            }
            //TODO: get the field reference from the ur
        }
//        System.out.println(response);


        String s = """
                  SELECT [System.Id], [System.Title], [System.Description], [System.WorkItemType], 
                                [System.State], [System.AssignedTo], [System.CreatedDate], [System.ChangedDate] 
                                FROM WorkItems WHERE [System.TeamProject] = '%s' AND 
                                ([System.Title] CONTAINS '%s' OR [System.Description] CONTAINS '%s' ) 
                                ORDER BY [System.ChangedDate] DESC
                """;
        s = """
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
        String formatted = s.formatted("Defect", "Test Defect");
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
        WorkItem workItem = workItemsRequestBuilder.get(26722, v -> v.queryParameters.expand = ALL);


        System.out.println(workItem);

    }

    @Test
    @Ignore
    public void test2() throws AzDException {
        String organisation = System.getProperty("o");

        String project = System.getProperty("p");
        String personalAccessToken = System.getProperty("t");


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
