package org.azdtasks.core;

import java.util.Date;
import java.util.List;

public record WorkItemComments(int workItemId, List<WorkItemComment> comments) {


    public record WorkItemComment(long id, String text, String author, Date createdDate) {
    }

    public WorkItemComments(int workItemId) {
        this(workItemId, List.of());
    }
}
