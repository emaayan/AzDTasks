package org.azd.workitemtracking.types.categories;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.azd.abstractions.serializer.SerializableCollectionEntity;

import java.util.List;

public class WorkItemTypeCategories extends SerializableCollectionEntity {

    @JsonProperty("count")
    private int count;

    @JsonProperty("value")
    private List<WorkItemTypeCategory> workItemTypeCategories;

    public int getCount() { return count; }

    public List<WorkItemTypeCategory> getWorkItemTypeCategories() { return workItemTypeCategories; }
}
