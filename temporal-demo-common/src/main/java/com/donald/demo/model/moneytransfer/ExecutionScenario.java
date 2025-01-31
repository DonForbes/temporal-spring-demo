package com.donald.demo.model.moneytransfer;

public enum ExecutionScenario {
    HAPPY_PATH,
    ADVANCED_VISIBILITY,
    HUMAN_IN_LOOP,
    API_DOWNTIME,
    BUG_IN_WORKFLOW,
    INVALID_ACCOUNT,
    FAIL_DEPOSIT
}
