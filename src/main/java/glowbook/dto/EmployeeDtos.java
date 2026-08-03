package glowbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public record EmployeeRequest(
            @NotBlank String employeeId,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String password,
            String phone,
            String email,
            Boolean active
    ) {
    }

    public record EmployeeResponse(
            String employeeId,
            String firstName,
            String lastName,
            String phone,
            String email,
            Boolean active
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
            String serviceName
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
