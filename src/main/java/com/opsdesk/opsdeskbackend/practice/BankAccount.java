package com.opsdesk.opsdeskbackend.practice;

public class BankAccount {

    private double balance;

    public BankAccount(double openingBalance) {
        if (openingBalance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        this.balance = openingBalance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit must be more than zero.");
        }
        balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal must be more than zero.");
        }
        if (amount > balance) {
            throw new IllegalArgumentException(
                    "Cannot withdraw " + amount + ": balance is only " + balance + ".");
        }
        balance -= amount;
    }

    public double getBalance() {
        return balance;
    }
}
