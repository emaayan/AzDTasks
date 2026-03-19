package org.azdtasks.core;

import java.util.Map;

public record WorkItemType(String name,String refName,String iconUrl, Map<String,String> states) {
}
