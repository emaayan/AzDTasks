package org.azdtasks.core;


import java.util.Date;
import java.util.Objects;

/**
 * Simplified model for Azure DevOps work items
 */
public record WorkItemModel(int id, String title, String description, String workItemType, String state,
                            String assignedTo, Date createdDate, Date changedDate,WorkItemComments workItemComments,String url) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkItemModel that = (WorkItemModel) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkItemModel{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", workItemType='" + workItemType + '\'' +
                ", state='" + state + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                '}';
    }
}
