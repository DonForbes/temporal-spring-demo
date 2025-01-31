package com.donald.demo.model.moneytransfer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Account {
    private String firstName;
    private String lastName;
    private String sortCode;
    private String accountNumber;
}
