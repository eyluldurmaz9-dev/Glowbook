package glowbook.repository;

import glowbook.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmployeeIdAndActiveTrue(String employeeId);

    Optional<Employee> findByEmailAndActiveTrue(String email);

    List<Employee> findByActiveTrueOrderByFirstNameAscLastNameAsc();
}
