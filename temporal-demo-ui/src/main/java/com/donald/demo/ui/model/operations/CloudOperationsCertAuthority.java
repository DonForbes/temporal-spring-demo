package com.donald.demo.ui.model.operations;

import lombok.Data;

import java.math.BigInteger;
import java.util.Collection;

@Data
public class CloudOperationsCertAuthority {
    private String caCert;
    private String expiryDate;
    private String notBefore;
    private String subjectPrincipal;
    private String issuerPrincipal;
    private Collection<String> alternativeNames;
    private BigInteger serialNumber;
}
