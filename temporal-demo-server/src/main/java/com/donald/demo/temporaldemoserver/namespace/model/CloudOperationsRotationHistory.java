package com.donald.demo.temporaldemoserver.namespace.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CloudOperationsRotationHistory {
    LocalDateTime auditDate;
    String auditType;
    String auditMessage;

    public CloudOperationsRotationHistory(LocalDateTime pAuditDate, String pAuditType, String pAuditMessage) {
        this.setAuditDate(pAuditDate);
        this.setAuditMessage(pAuditMessage);
        this.setAuditType(pAuditType);
    }

}
