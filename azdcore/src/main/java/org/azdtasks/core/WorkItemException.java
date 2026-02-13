package org.azdtasks.core;

import org.azd.exceptions.AzDException;

public class WorkItemException extends AzDException {
    private final int statusCode;

    public WorkItemException(Throwable cause) {
        super(cause);
        this.statusCode = -1;
    }

    public WorkItemException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public WorkItemException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
