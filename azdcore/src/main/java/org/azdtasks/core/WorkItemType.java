package org.azdtasks.core;

import java.util.Map;

public record WorkItemType(String name, Map<String,String> states) {
}
