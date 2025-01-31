package com.donald.demo.util;

import com.google.protobuf.Timestamp;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;

public class TemporalClient {

    public static Timestamp getOneHourAgo() {
        // Get current date-time in UTC
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        // Subtract one hour
        LocalDateTime oneHourAgo = now.minusHours(1);

        // Convert LocalDateTime to Instant
        Instant instant = oneHourAgo.atZone(ZoneId.of("UTC")).toInstant();

        // Convert Instant to Timestamp
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

        return timestamp;
    }

    public static String getWorkflowUrl(String workflowId, String target, String namespace) {
        String url = "";
        if (target.endsWith(".tmprl.cloud:7233")) {
            url = "https://cloud.temporal.io/namespaces/"
                    + namespace
                    + "/workflows/"
                    + workflowId;
        }
        return url;
    }
}
