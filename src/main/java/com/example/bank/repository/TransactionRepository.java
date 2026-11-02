package com.example.bank.repository;

import com.example.bank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("select t from Transaction t where t.fromAccount.id = :id or t.toAccount.id = :id order by t.createdAt desc, t.id desc")
    List<Transaction> findHistory(@Param("id") Long accountId);
}
