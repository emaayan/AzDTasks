package org.azdtasks.core;

import java.util.Set;

public record WorkItemTypeCategory(String name, String refName, String defWorkType, Set<String> workTypes ) {
}
