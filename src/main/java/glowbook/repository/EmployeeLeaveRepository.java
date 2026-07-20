package glowbook.repository;

import glowbook.entity.EmployeeLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Integer> {

    boolean existsByEmployeeEmployeeIdAndLeaveDate(String employeeId, LocalDate leaveDate);

    List<EmployeeLeave> findByEmployeeEmployeeIdAndLeaveDateBetween(
            String employeeId,
            LocalDate startDate,
            LocalDate endDate
    );
}
