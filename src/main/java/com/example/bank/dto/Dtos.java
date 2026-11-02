package com.example.bank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public class Dtos {
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 72) String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, String fullName, String accountNumber) {}

    public record AmountRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount) {}

    public record TransferRequest(
            @NotBlank String toAccountNumber,
            @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
            @Size(max = 255) String description) {}

    public record AccountResponse(String fullName, String accountNumber, BigDecimal balance) {}

    public record TransactionResponse(Long id, String type, BigDecimal amount, String status,
                                      String fromAccount, String toAccount, String description, Instant createdAt) {}
}
