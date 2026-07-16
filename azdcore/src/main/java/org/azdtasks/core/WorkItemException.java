package org.azdtasks.core;


import org.azd.enums.ApiExceptionTypes;
import org.azd.exceptions.AzDException;

public class WorkItemException extends Exception {
    private final int statusCode;

    private static String handle(AzDException azDException) {
        final String s = azDException.getMessage();
        final ApiExceptionTypes[] values = ApiExceptionTypes.values();
        for (ApiExceptionTypes value : values) {
            final String prefix = value.name() + ":";
            if (s.startsWith(prefix)) {
                return s.replace(prefix, "");
            }
        }
        return s;
    }


    public WorkItemException(AzDException exception) {
        this(handle(exception), exception);
    }

    public WorkItemException(Throwable cause) {
        super(cause);
        this.statusCode = -1;
    }

    public WorkItemException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public WorkItemException(String message, Throwable cause) {
        super(message, cause);
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
