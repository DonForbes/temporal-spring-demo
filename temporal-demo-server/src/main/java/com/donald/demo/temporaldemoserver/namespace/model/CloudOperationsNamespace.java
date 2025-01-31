package com.donald.demo.temporaldemoserver.namespace.model;

import lombok.Data;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudOperationsNamespace {
    private String name;
    private String activeRegion;
    //private String secondaryRegion;
    private String state;
    private int retentionPeriod;
    private Collection<CloudOperationsCertAuthority> certAuthorityPublicCerts;
    private String certAuthorityPublicCertificates;
    private Collection<CloudOperationsUser> cloudOpsUsers;
    private String codecEndPoint;
}
