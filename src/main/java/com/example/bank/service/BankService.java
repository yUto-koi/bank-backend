package com.example.bank.service;

import com.example.bank.dto.Dtos.*;
import com.example.bank.entity.*;
import com.example.bank.entity.Transaction.Status;
import com.example.bank.entity.Transaction.Type;
import com.example.bank.exception.ApiException;
import com.example.bank.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankService {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository txs;
    private final TokenStore tokens;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    // ---------- Auth ----------
    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email))
            throw new ApiException(HttpStatus.CONFLICT, "ဤ Email ဖြင့် အကောင့်ရှိပြီးသား ဖြစ်သည်");

        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        users.save(u);

        Account a = new Account();
        a.setUser(u);
        a.setAccountNumber(newAccountNumber());
        accounts.save(a);

        return new AuthResponse(tokens.issue(u.getId()), u.getFullName(), a.getAccountNumber());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email သို့မဟုတ် Password မှားနေသည်"));
        Account a = accounts.findByUserId(u.getId()).orElseThrow();
        return new AuthResponse(tokens.issue(u.getId()), u.getFullName(), a.getAccountNumber());
    }

    // ---------- Account ----------
    @Transactional(readOnly = true)
    public AccountResponse me(Long userId) {
        Account a = accounts.findByUserId(userId).orElseThrow(() -> notFound("Account"));
        return new AccountResponse(a.getUser().getFullName(), a.getAccountNumber(), a.getBalance());
    }

    @Transactional
    public AccountResponse deposit(Long userId, BigDecimal amount) {
        Account a = accounts.lockByUserId(userId).orElseThrow(() -> notFound("Account"));
        a.setBalance(a.getBalance().add(amount));
        record(Type.DEPOSIT, amount, null, a, "Deposit");
        return new AccountResponse(a.getUser().getFullName(), a.getAccountNumber(), a.getBalance());
    }

    @Transactional
    public AccountResponse withdraw(Long userId, BigDecimal amount) {
        Account a = accounts.lockByUserId(userId).orElseThrow(() -> notFound("Account"));
        if (a.getBalance().compareTo(amount) < 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "လက်ကျန်ငွေ မလုံလောက်ပါ");
        a.setBalance(a.getBalance().subtract(amount));
        record(Type.WITHDRAW, amount, a, null, "Withdraw");
        return new AccountResponse(a.getUser().getFullName(), a.getAccountNumber(), a.getBalance());
    }

    @Transactional
    public AccountResponse transfer(Long userId, TransferRequest r) {
        Long fromId = accounts.findIdByUserId(userId).orElseThrow(() -> notFound("Account"));
        Long toId = accounts.findIdByAccountNumber(r.toAccountNumber().trim())
                .orElseThrow(() -> notFound("လက်ခံမည့် Account"));
        if (fromId.equals(toId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "မိမိအကောင့်သို့ ငွေလွှဲ၍ မရပါ");

        // id အစဉ်လိုက် row lock ယူပြီးမှ လက်ကျန်ကို ဖတ်/ပြင်သည်
        List<Account> locked = accounts.lockAllById(List.of(fromId, toId));
        Account from = locked.stream().filter(a -> a.getId().equals(fromId)).findFirst().orElseThrow();
        Account to = locked.stream().filter(a -> a.getId().equals(toId)).findFirst().orElseThrow();

        if (from.getBalance().compareTo(r.amount()) < 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "လက်ကျန်ငွေ မလုံလောက်ပါ");

        from.setBalance(from.getBalance().subtract(r.amount()));
        to.setBalance(to.getBalance().add(r.amount()));
        record(Type.TRANSFER, r.amount(), from, to, r.description());
        return new AccountResponse(from.getUser().getFullName(), from.getAccountNumber(), from.getBalance());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> history(Long userId) {
        Long id = accounts.findIdByUserId(userId).orElseThrow(() -> notFound("Account"));
        return txs.findHistory(id).stream().map(t -> new TransactionResponse(
                t.getId(), t.getType().name(), t.getAmount(), t.getStatus().name(),
                t.getFromAccount() == null ? null : t.getFromAccount().getAccountNumber(),
                t.getToAccount() == null ? null : t.getToAccount().getAccountNumber(),
                t.getDescription(), t.getCreatedAt())).toList();
    }

    // ---------- helpers ----------
    private void record(Type type, BigDecimal amount, Account from, Account to, String desc) {
        Transaction t = new Transaction();
        t.setType(type);
        t.setAmount(amount);
        t.setFromAccount(from);
        t.setToAccount(to);
        t.setStatus(Status.SUCCESS);
        t.setDescription(desc);
        txs.save(t);
    }

    private String newAccountNumber() {
        String n;
        do {
            n = String.valueOf(1_000_000_000L + (long) (random.nextDouble() * 9_000_000_000L));
        } while (accounts.existsByAccountNumber(n));
        return n;
    }

    private ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, what + " ကို ရှာမတွေ့ပါ");
    }
}
