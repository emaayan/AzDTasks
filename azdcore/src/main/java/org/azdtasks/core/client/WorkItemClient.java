package org.azdtasks.core.client;


import org.azd.abstractions.ApiResponse;
import org.azd.abstractions.serializer.SerializableEntity;
import org.azd.authentication.AccessTokenCredential;
import org.azd.authentication.PersonalAccessTokenCredential;
import org.azd.core.CoreRequestBuilder;
import org.azd.core.types.*;
import org.azd.enums.HttpStatusCode;
import org.azd.enums.Instance;
import org.azd.enums.WorkItemExpand;
import org.azd.exceptions.AzDException;
import org.azd.http.ClientRequest;
import org.azd.serviceclient.AzDService;
import org.azd.serviceclient.AzDServiceClient;
import org.azd.workitemtracking.WorkItemTrackingRequestBuilder;
import org.azd.workitemtracking.types.*;
import org.azd.workitemtracking.wiql.WiqlRequestBuilder;
import org.azd.workitemtracking.workitems.WorkItemsRequestBuilder;
import org.azdtasks.core.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WorkItemClient extends AbstractWorkItemClient {


    public static CompletableFuture<Projects> getProjects(String org, String personalAccessToken) throws AzDException {
        //TODO: create logging
        final AzDServiceClient azDServiceClient = getClient(org, personalAccessToken);
        final CoreRequestBuilder core = azDServiceClient.core();
        final CompletableFuture<Projects> projectsCompletableFuture = core.projects().listAsync().thenApply(projects -> {
            try {
                return check(projects);
            } catch (WorkItemException e) {
                throw new RuntimeException(e);
            }
        });
        return projectsCompletableFuture;

    }

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


    private static AzDServiceClient getClient(String org, String personalAccessToken) {
        return getClient(org, null, personalAccessToken);
    }

    private static AzDServiceClient getClient(String org, String project, String personalAccessToken) {
        final PersonalAccessTokenCredential pat = new PersonalAccessTokenCredential(toURL(org), project, personalAccessToken);
        final AzDServiceClient service = AzDService.builder().authentication(pat).buildClient();
        return service;
    }

    private final WorkItemTrackingRequestBuilder wit;
    private final AzDServiceClient azDServiceClient;

    public WorkItemClient(String organization, String project, String personalAccessToken) {
        super(organization, project, personalAccessToken);
        final ConnectionConfig config = new ConnectionConfig(organization, project, personalAccessToken);
        azDServiceClient = getClient(config.organizationUrl(), config.project(), config.personalAccessToken());
        wit = azDServiceClient.workItemTracking();
    }


    @Override
    protected WorkItemList getWorkItems(int[] ids) throws AzDException {
        final WorkItemsRequestBuilder workItemsRequestBuilder = wit.workItems();
        return check(workItemsRequestBuilder.list(requestConfiguration -> {
            requestConfiguration.queryParameters.ids = Arrays.stream(ids).boxed().toArray(Integer[]::new);
            requestConfiguration.queryParameters.expand = WorkItemExpand.LINKS;
        }));
    }


    @Override
    protected WorkItemQueryResult query(String q, String team) throws AzDException {
        final WorkItemQueryResult workItemQueryResult = wit.wiql().query(team, q);
        return workItemQueryResult;
    }

    @Override
    protected Projects getProjectsImpl() throws AzDException {
        return azDServiceClient.core().projects().list();
    }

    @Override
    protected WebApiTeams getTeamsImpl() throws AzDException {
        return azDServiceClient.core().teams().list();
    }

    public WorkItemModel getWorkItem(int workItemId) throws AzDException {
        final WorkItemsRequestBuilder workItemsRequestBuilder = wit.workItems();
        final WorkItem workItem = check(workItemsRequestBuilder.get(workItemId, r -> r.queryParameters.expand = WorkItemExpand.ALL));
        final WorkItemModel workItemModel = convertToModel(workItem);
        return workItemModel;

    }

}
