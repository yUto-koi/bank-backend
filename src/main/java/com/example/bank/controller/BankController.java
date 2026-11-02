package com.example.bank.controller;

import com.example.bank.dto.Dtos.*;
import com.example.bank.exception.ApiException;
import com.example.bank.service.BankService;
import com.example.bank.service.TokenStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${app.cors.origin}")
@RequiredArgsConstructor
public class BankController {
    private final BankService bank;
    private final TokenStore tokens;

    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return bank.register(r); }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return bank.login(r); }

    @GetMapping("/account/me")
    public AccountResponse me(@RequestHeader(value = "Authorization", required = false) String h) {
        return bank.me(userId(h));
    }

    @PostMapping("/account/deposit")
    public AccountResponse deposit(@RequestHeader(value = "Authorization", required = false) String h,
                                   @Valid @RequestBody AmountRequest r) {
        return bank.deposit(userId(h), r.amount());
    }

    @PostMapping("/account/withdraw")
    public AccountResponse withdraw(@RequestHeader(value = "Authorization", required = false) String h,
                                    @Valid @RequestBody AmountRequest r) {
        return bank.withdraw(userId(h), r.amount());
    }

    @PostMapping("/account/transfer")
    public AccountResponse transfer(@RequestHeader(value = "Authorization", required = false) String h,
                                    @Valid @RequestBody TransferRequest r) {
        return bank.transfer(userId(h), r);
    }

    @GetMapping("/account/transactions")
    public List<TransactionResponse> history(@RequestHeader(value = "Authorization", required = false) String h) {
        return bank.history(userId(h));
    }

    private Long userId(String header) {
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        Long id = tokens.resolve(token);
        if (id == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "ပြန်လည် Login ဝင်ပါ");
        return id;
    }
}
