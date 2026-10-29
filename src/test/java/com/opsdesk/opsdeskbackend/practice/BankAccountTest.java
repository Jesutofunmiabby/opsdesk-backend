package com.opsdesk.opsdeskbackend.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BankAccountTest {

    @Test
    void depositIncreasesBalance() {
        BankAccount account = new BankAccount(100.0);

        account.deposit(50.0);

        assertEquals(150.0, account.getBalance());
    }

    @Test
    void negativeDepositThrows() {
        BankAccount account = new BankAccount(100.0);

        assertThrows(IllegalArgumentException.class, () -> account.deposit(-10.0));
    }

    @Test
    void withdrawingMoreThanBalanceThrows() {
        BankAccount account = new BankAccount(100.0);

        assertThrows(IllegalArgumentException.class, () -> account.withdraw(150.0));
    }
}
