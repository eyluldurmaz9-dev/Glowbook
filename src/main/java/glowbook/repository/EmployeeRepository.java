package glowbook.repository;

import glowbook.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmployeeIdAndActiveTrue(String employeeId);

    Optional<Employee> findByEmailAndActiveTrue(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Employee> findLockedByEmployeeIdAndActiveTrue(String employeeId);

    List<Employee> findByActiveTrueOrderByFirstNameAscLastNameAsc();
}
