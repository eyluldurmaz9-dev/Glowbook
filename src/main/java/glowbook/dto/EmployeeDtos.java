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
            @NotBlank(message = "Personel kimliği boş olamaz.") String employeeId,
            @NotBlank(message = "Ad boş olamaz.") String firstName,
            @NotBlank(message = "Soyad boş olamaz.") String lastName,
            String password,
            String phone,
            String email,
            Boolean active,
            Set<Integer> serviceIds,
            Set<Integer> optionIds
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
            @NotBlank(message = "Personel seçilmelidir.") String employeeId,
            @NotNull(message = "Hizmet seçilmelidir.") Integer serviceId
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
            Set<Integer> serviceIds,
            Set<Integer> optionIds
    ) {
    }

    public record EmployeeLeaveRequest(
            @NotBlank(message = "Personel seçilmelidir.") String employeeId,
            @NotNull(message = "İzin tarihi seçilmelidir.") LocalDate leaveDate,
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
