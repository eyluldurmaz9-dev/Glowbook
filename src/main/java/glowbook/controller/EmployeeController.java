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
import org.springframework.transaction.annotation.Transactional;

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
                .map(this::toEmployeeResponse)
                .toList());
    }

    @PostMapping
    @Transactional
    public ApiResponse<EmployeeDtos.EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeDtos.EmployeeRequest request) {
        Employee employee = toEmployee(request);
        Employee saved = employeeManagementService.create(employee);
        replaceAssignments(saved.getEmployeeId(), request.serviceIds(), request.optionIds());
        return ApiResponse.success("Employee created", toEmployeeResponse(saved));
    }

    @PutMapping("/{employeeId}")
    @Transactional
    public ApiResponse<EmployeeDtos.EmployeeResponse> updateEmployee(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeDtos.EmployeeRequest request
    ) {
        Employee saved = employeeManagementService.update(employeeId, toEmployee(request));
        replaceAssignments(employeeId, request.serviceIds(), request.optionIds());
        return ApiResponse.success("Employee updated", toEmployeeResponse(saved));
    }

    @DeleteMapping("/{employeeId}")
    public ApiResponse<EmployeeDtos.EmployeeResponse> deactivateEmployee(@PathVariable String employeeId) {
        return ApiResponse.success("Personel geçmiş randevuları korunarak pasifleştirildi", toEmployeeResponse(employeeManagementService.deactivate(employeeId)));
    }

    @PostMapping("/services")
    public ApiResponse<EmployeeDtos.EmployeeServiceResponse> assignService(@Valid @RequestBody EmployeeDtos.EmployeeServiceRequest request) {
        return ApiResponse.success("Employee service assigned", DtoMapper.toEmployeeServiceResponse(
                employeeServiceAssignmentService.assign(request.employeeId(), request.serviceId())
        ));
    }

    @GetMapping("/{employeeId}/services")
    public ApiResponse<List<EmployeeDtos.EmployeeServiceResponse>> getEmployeeServices(@PathVariable String employeeId) {
        return ApiResponse.success("Employee services listed", employeeServiceAssignmentService.getAssignments(employeeId)
                .stream()
                .map(DtoMapper::toEmployeeServiceResponse)
                .toList());
    }

    @PutMapping("/{employeeId}/services")
    public ApiResponse<List<EmployeeDtos.EmployeeServiceResponse>> replaceEmployeeServices(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeDtos.EmployeeAssignmentsRequest request
    ) {
        return ApiResponse.success("Employee services updated", replaceAssignments(
                employeeId, request.serviceIds(), request.optionIds()).stream()
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

    private EmployeeDtos.EmployeeResponse toEmployeeResponse(Employee employee) {
        return DtoMapper.toEmployeeResponse(employee,
                employeeServiceAssignmentService.getAssignments(employee.getEmployeeId()));
    }

    private List<glowbook.entity.EmployeeService> replaceAssignments(
            String employeeId,
            java.util.Set<Integer> serviceIds,
            java.util.Set<Integer> optionIds
    ) {
        if (serviceIds != null) {
            return employeeServiceAssignmentService.replaceServiceAssignments(employeeId, serviceIds);
        }
        return employeeServiceAssignmentService.replaceOptionAssignments(employeeId, optionIds);
    }
}
