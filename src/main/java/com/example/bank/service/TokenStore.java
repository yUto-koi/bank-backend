package com.example.bank.service;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** စမ်းသပ်ရန်သာ - Memory ထဲတွင် token သိမ်းသည် (Server restart လျှင် ပျောက်မည်)။ Production တွင် JWT သုံးပါ။ */
@Component
public class TokenStore {
    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        String t = UUID.randomUUID().toString();
        tokens.put(t, userId);
        return t;
    }

    public Long resolve(String token) { return token == null ? null : tokens.get(token); }
}
