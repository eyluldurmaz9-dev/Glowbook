package glowbook.controller;

import glowbook.dto.ApiResponse;
import glowbook.dto.AppointmentDtos;
import glowbook.dto.DtoMapper;
import glowbook.entity.Appointment;
import glowbook.entity.Customer;
import glowbook.entity.CustomerPackage;
import glowbook.entity.Employee;
import glowbook.entity.ServiceOption;
import glowbook.security.AuthorizationSupport;
import glowbook.security.UserRole;
import glowbook.service.AppointmentAlgorithmService;
import glowbook.service.AppointmentService;
import glowbook.service.WaitingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentAlgorithmService appointmentAlgorithmService;
    private final WaitingListService waitingListService;
    private final AuthorizationSupport authorizationSupport;

    @GetMapping("/available-slots")
    public ApiResponse<List<AppointmentDtos.AvailableSlotResponse>> getAvailableSlots(
            @RequestParam Integer serviceId,
            @RequestParam LocalDate date
    ) {
        return ApiResponse.success("Available slots listed", appointmentAlgorithmService.findAvailableSlots(serviceId, date));
    }

    @PostMapping
    public ApiResponse<AppointmentDtos.AppointmentResponse> create(
            @Valid @RequestBody AppointmentDtos.AppointmentRequest request,
            Authentication authentication
    ) {
        if (request.customerId() != null) {
            authorizationSupport.assertCustomerCanAccess(request.customerId(), authentication);
        }
        Appointment appointment = appointmentService.create(toAppointment(request));
        return ApiResponse.success("Appointment created", DtoMapper.toAppointmentResponse(appointment));
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AppointmentDtos.AppointmentResponse> getById(
            @PathVariable Integer appointmentId,
            Authentication authentication
    ) {
        Appointment appointment = appointmentService.getById(appointmentId);
        assertCanAccessAppointment(appointment, authentication);
        return ApiResponse.success("Appointment found", DtoMapper.toAppointmentResponse(appointment));
    }

    @GetMapping("/customer/{customerId}/upcoming")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ApiResponse<List<AppointmentDtos.AppointmentResponse>> getUpcoming(
            @PathVariable Integer customerId,
            @RequestParam(required = false) LocalDate fromDate,
            Authentication authentication
    ) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        LocalDate date = fromDate == null ? LocalDate.now() : fromDate;
        return ApiResponse.success("Upcoming appointments listed", appointmentService.getUpcomingCustomerAppointments(customerId, date)
                .stream()
                .map(DtoMapper::toAppointmentResponse)
                .toList());
    }

    @GetMapping("/customer/{customerId}/past")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ApiResponse<List<AppointmentDtos.AppointmentResponse>> getPast(
            @PathVariable Integer customerId,
            @RequestParam(required = false) LocalDate beforeDate,
            Authentication authentication
    ) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        LocalDate date = beforeDate == null ? LocalDate.now() : beforeDate;
        return ApiResponse.success("Past appointments listed", appointmentService.getPastCustomerAppointments(customerId, date)
                .stream()
                .map(DtoMapper::toAppointmentResponse)
                .toList());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<List<AppointmentDtos.AppointmentResponse>> getEmployeeSchedule(
            @PathVariable String employeeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication
    ) {
        authorizationSupport.assertEmployeeCanAccess(employeeId, authentication);
        return ApiResponse.success("Employee schedule listed", appointmentService.getEmployeeSchedule(employeeId, startDate, endDate)
                .stream()
                .map(DtoMapper::toAppointmentResponse)
                .toList());
    }

    @PatchMapping("/{appointmentId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<AppointmentDtos.AppointmentResponse> approve(@PathVariable Integer appointmentId) {
        return ApiResponse.success("Appointment approved", DtoMapper.toAppointmentResponse(appointmentService.approve(appointmentId)));
    }

    @PatchMapping("/{appointmentId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<AppointmentDtos.AppointmentResponse> complete(@PathVariable Integer appointmentId) {
        return ApiResponse.success("Appointment completed", DtoMapper.toAppointmentResponse(appointmentService.complete(appointmentId)));
    }

    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AppointmentDtos.AppointmentResponse> cancel(
            @PathVariable Integer appointmentId,
            @RequestBody AppointmentDtos.CancelAppointmentRequest request,
            Authentication authentication
    ) {
        assertCanAccessAppointment(appointmentService.getById(appointmentId), authentication);
        Appointment appointment = appointmentService.cancel(appointmentId, request.cancellationReason());
        waitingListService.notifyMatchingCustomers(appointment.getService().getServiceId(), appointment.getAppointmentDate());
        return ApiResponse.success("Appointment cancelled", DtoMapper.toAppointmentResponse(appointment));
    }

    @PatchMapping("/{appointmentId}/time")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<AppointmentDtos.AppointmentResponse> updateTime(
            @PathVariable Integer appointmentId,
            @Valid @RequestBody AppointmentDtos.AppointmentTimeUpdateRequest request
    ) {
        Appointment appointment = Appointment.builder()
                .appointmentDate(request.appointmentDate())
                .appointmentTime(request.appointmentTime())
                .build();
        return ApiResponse.success("Appointment time updated", DtoMapper.toAppointmentResponse(appointmentService.updateTime(appointmentId, appointment)));
    }

    private Appointment toAppointment(AppointmentDtos.AppointmentRequest request) {
        return Appointment.builder()
                .customer(request.customerId() == null ? null : Customer.builder().customerId(request.customerId()).build())
                .customerPackage(request.customerPackageId() == null ? null : CustomerPackage.builder().customerPackageId(request.customerPackageId()).build())
                .employee(Employee.builder().employeeId(request.employeeId()).build())
                .service(glowbook.entity.Service.builder().serviceId(request.serviceId()).build())
                .serviceOption(ServiceOption.builder().optionId(request.optionId()).build())
                .customerName(request.customerName())
                .customerSurname(request.customerSurname())
                .phone(request.phone())
                .appointmentDate(request.appointmentDate())
                .appointmentTime(request.appointmentTime())
                .build();
    }

    private void assertCanAccessAppointment(Appointment appointment, Authentication authentication) {
        if (appointment.getCustomer() != null) {
            authorizationSupport.assertCustomerCanAccess(appointment.getCustomer().getCustomerId(), authentication);
            return;
        }
        if (!authorizationSupport.hasAnyRole(authentication, UserRole.ADMIN, UserRole.EMPLOYEE)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
    }
}
