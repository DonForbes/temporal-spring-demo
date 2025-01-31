package com.donald.demo.model.moneytransfer;

public enum TransferState {
    NEW,
    APPROVAL_TIMED_OUT,
    APPROVED,
    VALIDATION_FAILED,
    VALIDATED,
    FUNDS_WITHDRAWN,
    WITHDRAW_FAILED,
    FUNDS_DEPOSITED,
    DEPOSIT_FAILED,
    COMPLETED,
    COMPLETED_WITH_FAILURE
}
