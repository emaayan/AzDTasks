package org.azd.workitemtracking.types.categories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.azd.abstractions.serializer.SerializableEntity;

// A lightweight reference to a work item type (name + url only)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkItemTypeReference extends SerializableEntity {

    @JsonProperty("name")
    private String name;   // e.g. "Bug", "Defect", "Custom Bug"

    @JsonProperty("url")
    private String url;

    public String getName() { return name; }
    public String getUrl() { return url; }
}