package com.donald.demo.temporaldemoserver.namespace.workflows;

import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;
import com.donald.demo.temporaldemoserver.namespace.model.WorkflowMetadata;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DeleteNamespace {
    @WorkflowMethod
    public String deleteNamespace( WorkflowMetadata wfMetadata, CloudOperationsNamespace cloudOpsNamespace);

    @SignalMethod
    public void setApproved();

    @QueryMethod
    public CloudOperationsNamespace getNamespaceDetails();

    @QueryMethod
    public WorkflowMetadata getWFMetadata();
}
