package glowbook.repository;

import glowbook.entity.EmployeeService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmployeeServiceRepository extends JpaRepository<EmployeeService, Integer> {

    List<EmployeeService> findByServiceServiceIdAndEmployeeActiveTrue(Integer serviceId);

    List<EmployeeService> findByEmployeeEmployeeIdOrderByServiceServiceNameAsc(String employeeId);

    @Query("""
            select assignment from EmployeeService assignment
            where assignment.service.serviceId = :serviceId
              and assignment.employee.active = true
              and (assignment.serviceOption is null or assignment.serviceOption.optionId = :optionId)
            order by assignment.employee.firstName, assignment.employee.lastName
            """)
    List<EmployeeService> findQualifiedActiveEmployees(
            @Param("serviceId") Integer serviceId,
            @Param("optionId") Integer optionId
    );

    boolean existsByEmployeeEmployeeIdAndServiceServiceId(String employeeId, Integer serviceId);

    /** Data-cleanup merge finders: re-point every assignment off a duplicate catalog row before it is deleted. */
    List<EmployeeService> findByServiceServiceId(Integer serviceId);

    List<EmployeeService> findByServiceOptionOptionId(Integer optionId);

    @Query("""
            select (count(assignment) > 0) from EmployeeService assignment
            where assignment.employee.employeeId = :employeeId
              and assignment.service.serviceId = :serviceId
              and (assignment.serviceOption is null or assignment.serviceOption.optionId = :optionId)
            """)
    boolean employeeCanProvideOption(
            @Param("employeeId") String employeeId,
            @Param("serviceId") Integer serviceId,
            @Param("optionId") Integer optionId
    );

    @Transactional
    void deleteByEmployeeEmployeeId(String employeeId);
}
