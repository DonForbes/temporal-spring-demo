package com.donald.demo.temporaldemoserver.namespace.workflows;

import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;
import com.donald.demo.temporaldemoserver.namespace.model.WorkflowMetadata;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;


@WorkflowInterface
public interface ManageNamespace {

    @WorkflowMethod
    public CloudOperationsNamespace manageNamespace(WorkflowMetadata wfMetadata, CloudOperationsNamespace cloudOpsNamespace);

    @QueryMethod
    public CloudOperationsNamespace getNamespaceDetails();

    @QueryMethod
    public WorkflowMetadata getWFMetadata();

    @SignalMethod
    public void setPageDisplay(WorkflowMetadata wfMetadata);

    @SignalMethod
    public void setNamespace(CloudOperationsNamespace cloudOpsNamespace);

    @SignalMethod
    public void createOrUpdateNamespace(CloudOperationsNamespace cloudOpsNamespace);

}
