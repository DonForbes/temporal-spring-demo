package com.donald.demo.ui.model.operations;

import java.util.Collection;

import lombok.Data;

@Data
public class CloudOperationsUser {
    private String eMail;
    private String id;
    private Collection<CloudOperationsNamespaceAccess> cloudOpsNamespaceAccess;
    private String role;
}
