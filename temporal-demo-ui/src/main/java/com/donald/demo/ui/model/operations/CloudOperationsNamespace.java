package com.donald.demo.ui.model.operations;

import java.util.ArrayList;
import java.util.Collection;
import lombok.Data;

@Data
public class CloudOperationsNamespace {
    private String name;
    private String activeRegion;
    private String state;
    private int retentionPeriod;
    private Collection<CloudOperationsCertAuthority> certAuthorityPublicCerts = new ArrayList<>();
    private String certAuthorityPublicCertificates;
    private Collection<CloudOperationsUser> cloudOpsUsers;
    private String codecEndPoint;
//    private int formPage;

    public static CloudOperationsNamespace build(){
        CloudOperationsNamespace cloudOpsNs = new CloudOperationsNamespace();
        return cloudOpsNs;
    }
    
 /**    
  * public String getCertAuthorityPublicCertificates() {
        StringBuilder returnCerts = new StringBuilder();
        if (this.getCertAuthorityPublicCerts() != null) {
            for (CloudOperationsCertAuthority ca : this.certAuthorityPublicCerts) {
                returnCerts.append(ca.getCaCert()).append(System.lineSeparator());
            }
        }
        return returnCerts.toString();
    }

    public void setCertAuthorityPublicCertificates(String caCerts) {
        // For now we will just accept all content in this field and set the string
        // value of the first ca public cert to the value
        // provided. Going forward this will need to be split up into mulitple certs,
        // pem and then base64 encoded no doubt.
        CloudOperationsCertAuthority cloudOpsCA = new CloudOperationsCertAuthority();
        cloudOpsCA.setCaCert(caCerts);
        Collection<CloudOperationsCertAuthority> CACerts;
        if (this.getCertAuthorityPublicCerts() == null) {
            CACerts = new ArrayList<>();
        } else
            CACerts = this.getCertAuthorityPublicCerts();

        CACerts.add(cloudOpsCA);
        this.setCertAuthorityPublicCerts(CACerts);

        certAuthorityPublicCertificates = CACerts.toString();

    }
        */
}
