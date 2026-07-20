package glowbook.repository;

import glowbook.entity.EmployeeService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeServiceRepository extends JpaRepository<EmployeeService, Integer> {

    List<EmployeeService> findByServiceServiceIdAndEmployeeActiveTrue(Integer serviceId);

    boolean existsByEmployeeEmployeeIdAndServiceServiceId(String employeeId, Integer serviceId);
}
