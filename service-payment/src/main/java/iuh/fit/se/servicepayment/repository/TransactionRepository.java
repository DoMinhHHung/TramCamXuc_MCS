package iuh.fit.se.servicepayment.repository;

import iuh.fit.se.servicepayment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    /**
 * Finds the transaction associated with the given order code.
 *
 * @param orderCode the order code to search by
 * @return an Optional containing the matching Transaction if found, or an empty Optional if no match exists
 */
Optional<Transaction> findByOrderCode(Long orderCode);
}