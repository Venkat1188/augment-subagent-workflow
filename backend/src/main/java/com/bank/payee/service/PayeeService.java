package com.bank.payee.service;

import com.bank.payee.model.Payee;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PayeeService {

    private final Map<String, Payee> payeeStore = new ConcurrentHashMap<>();

    /**
     * Persists a confirmed payee. Assigns a unique ID and addedAt timestamp.
     *
     * @param payee the payee to store
     * @return the stored payee (with id and addedAt populated)
     */
    public Payee addPayee(Payee payee) {
        payee.setId(UUID.randomUUID().toString());
        payee.setAddedAt(LocalDateTime.now());
        payeeStore.put(payee.getId(), payee);
        return payee;
    }

    /**
     * Returns all confirmed payees.
     */
    public List<Payee> getPayees() {
        return new ArrayList<>(payeeStore.values());
    }
}
