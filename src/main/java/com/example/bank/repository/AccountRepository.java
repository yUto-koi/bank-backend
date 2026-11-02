package com.example.bank.repository;

import com.example.bank.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);

    @Query("select a from Account a where a.user.id = :uid")
    Optional<Account> findByUserId(@Param("uid") Long uid);

    @Query("select a.id from Account a where a.user.id = :uid")
    Optional<Long> findIdByUserId(@Param("uid") Long uid);

    @Query("select a.id from Account a where a.accountNumber = :no")
    Optional<Long> findIdByAccountNumber(@Param("no") String no);

    /** Row lock (SELECT ... FOR UPDATE) - ငွေနှစ်ဆထုတ်/Race condition မဖြစ်စေရန် */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.user.id = :uid")
    Optional<Account> lockByUserId(@Param("uid") Long uid);

    /** id အစဉ်လိုက် lock ယူခြင်းဖြင့် Transfer နှစ်ခုဆန့်ကျင်ဘက် Deadlock မဖြစ်စေရန် */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAllById(@Param("ids") List<Long> ids);
}
