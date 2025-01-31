package com.donald.demo.temporaldemoserver.transfermoney.util;
import java.util.Calendar;
import java.util.TimeZone;

import io.temporal.workflow.Workflow;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class IdGenerator {

    private static final String defaultDateFormat = "MMdd-HHmmss";

    public static String generateWorkflowId() {
        String timeStamp = new SimpleDateFormat(defaultDateFormat).format(Calendar.getInstance().getTime());
        return String.format( "TRANSFER-%s%03d",
                timeStamp + "-"
                + (char) (Math.random() * 26 + 'A')
                + ""
                + (char) (Math.random()* 26 + 'A')
                + ""
                + (char) (Math.random() * 26 + 'A'),
            (int) (Math.random() * 999));
      }

    public static String generateTransferId() {
        String timeStamp = returnFormattedWorkflowDate(null);
        return String.format("TSFR-%s%03d",
                timeStamp + "-"
                + (char) (Workflow.newRandom().nextDouble() * 26 + 'A')
                 + ""
                 + (char) (Workflow.newRandom().nextDouble() * 26 + 'A')
                 + ""
                 + (char) (Workflow.newRandom().nextDouble() * 26 + 'A'),
                (int) (Workflow.newRandom().nextDouble()  * 999));
    }  //End generateTransferId

    public static String returnFormattedWorkflowDate(String format) {
        
        LocalDateTime workflowTime = java.time.LocalDateTime.ofInstant(Instant.ofEpochMilli(Workflow.currentTimeMillis()), ZoneId.systemDefault());
        if (format == null)
            format = defaultDateFormat;
        return workflowTime.format(DateTimeFormatter.ofPattern(format));
    }
}
