package com.donald.demo.temporaldemoserver.namespace.activities;

import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface NamespaceManagement {

    @ActivityMethod
    public CloudOperationsNamespace getExistingNamespace(CloudOperationsNamespace cloudOpsNamespace, String apiKey);

    @ActivityMethod
    public String createNamespace(CloudOperationsNamespace cloudOpsNamespace, String apiKey);

    @ActivityMethod
    public String updateNamespace(CloudOperationsNamespace cloudOpsNamespace, String apiKey);

    @ActivityMethod
    public String emailChanges(CloudOperationsNamespace cloudOpsNamespace);
   
    @ActivityMethod
    public String emailFailure(CloudOperationsNamespace cloudOpsNamespace);

    @ActivityMethod
    public String deleteNamespace(CloudOperationsNamespace cloudOpsNamespace, String apiKey);

}
