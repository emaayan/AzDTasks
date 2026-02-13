package org.azdtasks.core;

import java.util.Date;
import java.util.List;

public record WorkItemComments(List<WorkItemComment> comments) {


    public record WorkItemComment(long id, String text, String author, Date createdDate) {
    }
}
