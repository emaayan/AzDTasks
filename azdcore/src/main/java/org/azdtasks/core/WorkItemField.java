package org.azdtasks.core;

import org.azd.enums.FieldType;

public record WorkItemField(String id, String name,String description, String type,boolean isReadOnly) {

    public boolean isSystem() {
        return id.startsWith("System.") || id.startsWith("Microsoft.VSTS");
    }

    public boolean isCustom() {
        return id.startsWith("Custom.");
    }

    public boolean isNumeric() {
        return FieldType.DOUBLE.name().equals(type) || FieldType.INTEGER.name().equals(type);
    }

    public boolean isString() {
        return FieldType.PLAINTEXT.name().equals(type) || FieldType.STRING.name().equals(type);
    }
}
