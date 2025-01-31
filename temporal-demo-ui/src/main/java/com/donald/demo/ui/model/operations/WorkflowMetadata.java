package com.donald.demo.ui.model.operations;

import lombok.Data;

@Data
public class WorkflowMetadata {
    private String apiKey;
    private Boolean isNewNamespace;
    private Boolean nsDataGathered;
    private Boolean approved;
    private int manageNamespaceTimeoutMins=11;
    private int pageDisplay=1;
}
