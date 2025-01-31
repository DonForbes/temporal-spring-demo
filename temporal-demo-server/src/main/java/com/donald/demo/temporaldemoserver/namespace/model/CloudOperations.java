package com.donald.demo.temporaldemoserver.namespace.model;

import lombok.Data;

@Data
public class CloudOperations {
    private CloudOperationsNamespace cloudOpsNamespace;
    private WorkflowMetadata wfMetadata;
}
