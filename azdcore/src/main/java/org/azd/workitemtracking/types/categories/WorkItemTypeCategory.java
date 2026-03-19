package org.azd.workitemtracking.types.categories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.azd.abstractions.serializer.SerializableEntity;

import java.util.List;

// Represents a single category (e.g. "Bug Category")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkItemTypeCategory extends SerializableEntity {

    @JsonProperty("name")
    private String name;                        // e.g. "Bug Category"

    @JsonProperty("referenceName")
    private String referenceName;               // e.g. "Microsoft.BugCategory"

    @JsonProperty("defaultWorkItemType")
    private WorkItemTypeReference defaultWorkItemType;

    @JsonProperty("workItemTypes")
    private List<WorkItemTypeReference> workItemTypes;

    @JsonProperty("url")
    private String url;

    public String getName() { return name; }
    public String getReferenceName() { return referenceName; }
    public WorkItemTypeReference getDefaultWorkItemType() { return defaultWorkItemType; }
    public List<WorkItemTypeReference> getWorkItemTypes() { return workItemTypes; }
    public String getUrl() { return url; }

//    // Convenience: check if this is a specific known category
//    public boolean isBugCategory() {
//        return "Microsoft.BugCategory".equalsIgnoreCase(referenceName);
//    }
//
//    public boolean isRequirementCategory() {
//        return "Microsoft.RequirementCategory".equalsIgnoreCase(referenceName);
//    }
//
//    public boolean isTaskCategory() {
//        return "Microsoft.TaskCategory".equalsIgnoreCase(referenceName);
//    }
}