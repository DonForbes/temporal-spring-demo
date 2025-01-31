package com.donald.demo.ui.model.operations;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class CloudOperationsSchedule {
    private String type = "INTERVAL";
    private long scheduleFrequency;
    private String scheduleUnits;
    @JsonIgnore
    private Duration Duration;

    public Duration getDuration() {
        if (this.getScheduleFrequency() < 1) // Not much front end validation so if the frequency is <1 set it to 1
            this.setScheduleFrequency(1);

        // Returns a duration based on the values of the schedule
        switch (scheduleUnits) {
            case "days":
                return Duration.ofDays(this.getScheduleFrequency());
            case "hours":
                return Duration.ofHours(this.getScheduleFrequency());
            case "minutes":
                return Duration.ofMinutes(this.getScheduleFrequency());
            default:
                return Duration.ofDays(this.getScheduleFrequency());
        }
    } // End getDuration
}
