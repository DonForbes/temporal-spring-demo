package com.donald.demo.temporaldemoserver.namespace.activities;

import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsCertAuthority;
import com.donald.demo.temporaldemoserver.namespace.model.CloudOperationsNamespace;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CertificateManagement {

    @ActivityMethod
    public CloudOperationsCertAuthority getCurrentCACert(); 


}
