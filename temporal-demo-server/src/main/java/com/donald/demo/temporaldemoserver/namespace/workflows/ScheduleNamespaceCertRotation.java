package com.donald.demo.temporaldemoserver.namespace.workflows;


import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsRotationHistory;
import com.donald.demo.temporaldemoserver.namespace.model.WorkflowMetadata;


import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Collection;

@WorkflowInterface
public interface ScheduleNamespaceCertRotation {

    @WorkflowMethod
    public void checkAndRotateCACertificates(CloudOperationsNamespace cloudOpsNamespace, WorkflowMetadata wfMetadata);

    @QueryMethod
    public CloudOperationsNamespace getNamespaceDetails();

    @QueryMethod
    public Collection<CloudOperationsRotationHistory> getAuditHistory();
}
