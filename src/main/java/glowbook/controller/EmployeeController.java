package glowbook.controller;

import glowbook.dto.ApiResponse;
import glowbook.dto.DtoMapper;
import glowbook.dto.EmployeeDtos;
import glowbook.entity.Employee;
import glowbook.entity.EmployeeLeave;
import glowbook.service.EmployeeLeaveService;
import glowbook.service.EmployeeManagementService;
import glowbook.service.EmployeeServiceAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {

    private final EmployeeManagementService employeeManagementService;
    private final EmployeeServiceAssignmentService employeeServiceAssignmentService;
    private final EmployeeLeaveService employeeLeaveService;

    @GetMapping
    public ApiResponse<List<EmployeeDtos.EmployeeResponse>> getEmployees() {
        return ApiResponse.success("Employees listed", employeeManagementService.getAllEmployees()
                .stream()
                .map(DtoMapper::toEmployeeResponse)
                .toList());
    }

    @PostMapping
    public ApiResponse<EmployeeDtos.EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeDtos.EmployeeRequest request) {
        Employee employee = toEmployee(request);
        return ApiResponse.success("Employee created", DtoMapper.toEmployeeResponse(employeeManagementService.create(employee)));
    }

    @PutMapping("/{employeeId}")
    public ApiResponse<EmployeeDtos.EmployeeResponse> updateEmployee(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeDtos.EmployeeRequest request
    ) {
        return ApiResponse.success("Employee updated", DtoMapper.toEmployeeResponse(employeeManagementService.update(employeeId, toEmployee(request))));
    }

    @DeleteMapping("/{employeeId}")
    public ApiResponse<EmployeeDtos.EmployeeResponse> deactivateEmployee(@PathVariable String employeeId) {
        return ApiResponse.success("Employee deactivated", DtoMapper.toEmployeeResponse(employeeManagementService.deactivate(employeeId)));
    }

    @PostMapping("/services")
    public ApiResponse<EmployeeDtos.EmployeeServiceResponse> assignService(@Valid @RequestBody EmployeeDtos.EmployeeServiceRequest request) {
        return ApiResponse.success("Employee service assigned", DtoMapper.toEmployeeServiceResponse(
                employeeServiceAssignmentService.assign(request.employeeId(), request.serviceId())
        ));
    }

    @GetMapping("/services/{serviceId}")
    public ApiResponse<List<EmployeeDtos.EmployeeServiceResponse>> getEmployeesByService(@PathVariable Integer serviceId) {
        return ApiResponse.success("Employees by service listed", employeeServiceAssignmentService.getEmployeesByService(serviceId)
                .stream()
                .map(DtoMapper::toEmployeeServiceResponse)
                .toList());
    }

    @PostMapping("/leaves")
    public ApiResponse<EmployeeDtos.EmployeeLeaveResponse> createLeave(@Valid @RequestBody EmployeeDtos.EmployeeLeaveRequest request) {
        EmployeeLeave leave = EmployeeLeave.builder()
                .leaveDate(request.leaveDate())
                .reason(request.reason())
                .build();
        return ApiResponse.success("Employee leave created", DtoMapper.toEmployeeLeaveResponse(employeeLeaveService.create(request.employeeId(), leave)));
    }

    @GetMapping("/{employeeId}/leaves")
    public ApiResponse<List<EmployeeDtos.EmployeeLeaveResponse>> getLeaves(
            @PathVariable String employeeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ApiResponse.success("Employee leaves listed", employeeLeaveService.getLeaves(employeeId, startDate, endDate)
                .stream()
                .map(DtoMapper::toEmployeeLeaveResponse)
                .toList());
    }

    private Employee toEmployee(EmployeeDtos.EmployeeRequest request) {
        return Employee.builder()
                .employeeId(request.employeeId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .password(request.password())
                .phone(request.phone())
                .email(request.email())
                .active(request.active() == null || request.active())
                .build();
    }
}
