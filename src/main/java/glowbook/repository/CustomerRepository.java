package glowbook.repository;

import glowbook.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Customer> findLockedByCustomerId(Integer customerId);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);
}
