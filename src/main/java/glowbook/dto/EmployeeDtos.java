package glowbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public record EmployeeRequest(
            @NotBlank String employeeId,
            @NotBlank String firstName,
            @NotBlank String lastName,
            String password,
            String phone,
            String email,
            Boolean active,
            @NotNull Set<Integer> optionIds
    ) {
    }

    public record EmployeeResponse(
            String employeeId,
            String firstName,
            String lastName,
            String phone,
            String email,
            Boolean active,
            Integer assignedServiceCount,
            List<EmployeeServiceResponse> assignedServices
    ) {
    }

    public record EmployeeServiceRequest(
            @NotBlank String employeeId,
            @NotNull Integer serviceId
    ) {
    }

    public record EmployeeServiceResponse(
            Integer employeeServiceId,
            String employeeId,
            String employeeName,
            Integer serviceId,
            String serviceName,
            Integer optionId,
            String optionName
    ) {
    }

    public record EmployeeAssignmentsRequest(
            @NotNull Set<Integer> optionIds
    ) {
    }

    public record EmployeeLeaveRequest(
            @NotBlank String employeeId,
            @NotNull LocalDate leaveDate,
            String reason
    ) {
    }

    public record EmployeeLeaveResponse(
            Integer leaveId,
            String employeeId,
            LocalDate leaveDate,
            String reason
    ) {
    }
}
