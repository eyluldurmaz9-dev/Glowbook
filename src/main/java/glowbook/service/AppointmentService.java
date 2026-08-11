package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import glowbook.entity.Customer;
import glowbook.entity.CustomerPackage;
import glowbook.entity.Employee;
import glowbook.entity.NotificationType;
import glowbook.entity.ServiceOption;
import glowbook.exception.BusinessException;
import glowbook.exception.ConflictException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private static final Set<AppointmentStatus> BLOCKING_STATUSES = Set.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.APPROVED
    );

    private final AppointmentRepository appointmentRepository;
    private final CustomerService customerService;
    private final EmployeeManagementService employeeManagementService;
    private final ServiceCatalogService serviceCatalogService;
    private final ServiceOptionService serviceOptionService;
    private final CustomerPackageService customerPackageService;
    private final EmployeeServiceAssignmentService employeeServiceAssignmentService;
    private final WorkingHourService workingHourService;
    private final EmployeeLeaveService employeeLeaveService;
    private final HolidayService holidayService;
    private final AppointmentAlgorithmService appointmentAlgorithmService;
    private final NotificationService notificationService;

    public Appointment getById(Integer appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
    }

    public List<Appointment> getEmployeeSchedule(String employeeId, LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByEmployeeEmployeeIdAndAppointmentDateBetweenAndStatusInOrderByAppointmentDateAscAppointmentTimeAsc(
                employeeId,
                startDate,
                endDate,
                BLOCKING_STATUSES
        );
    }

    public List<Appointment> getUpcomingCustomerAppointments(Integer customerId, LocalDate fromDate) {
        return appointmentRepository.findByCustomerCustomerIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAsc(
                customerId,
                fromDate
        );
    }

    public List<Appointment> getPastCustomerAppointments(Integer customerId, LocalDate beforeDate) {
        return appointmentRepository.findByCustomerCustomerIdAndAppointmentDateLessThanOrderByAppointmentDateDescAppointmentTimeDesc(
                customerId,
                beforeDate
        );
    }

    @Transactional
    public Appointment create(Appointment request) {
        Employee employee = employeeManagementService.getActiveByIdForBooking(request.getEmployee().getEmployeeId());
        glowbook.entity.Service service = serviceCatalogService.getActiveById(request.getService().getServiceId());
        ServiceOption option = serviceOptionService.getActiveByService(service.getServiceId(), request.getServiceOption().getOptionId());

        appointmentAlgorithmService.validateSlot(
                employee.getEmployeeId(),
                service.getServiceId(),
                request.getAppointmentDate(),
                request.getAppointmentTime()
        );
        validateAvailability(employee.getEmployeeId(), service.getServiceId(), request);

        Customer customer = resolveCustomer(request);
        CustomerPackage customerPackage = resolveAndUseCustomerPackage(request, customer, service.getServiceId());

        request.setEmployee(employee);
        request.setService(service);
        request.setServiceOption(option);
        request.setCustomer(customer);
        request.setCustomerPackage(customerPackage);
        request.setPrice(BigDecimal.valueOf(option.getPrice()));
        if (request.getStatus() == null) {
            request.setStatus(AppointmentStatus.PENDING);
        }

        Appointment appointment = appointmentRepository.save(request);
        notificationService.createAndSendSmsSafely(
                appointment.getCustomer(),
                appointment,
                NotificationType.APPOINTMENT_CREATED,
                "Randevu olusturuldu",
                "GlowBook randevunuz olusturuldu: " + appointment.getAppointmentDate() + " " + appointment.getAppointmentTime(),
                appointment.getPhone()
        );
        return appointment;
    }

    @Transactional
    public Appointment approve(Integer appointmentId) {
        Appointment appointment = getById(appointmentId);
        ensureNotCancelled(appointment);
        appointment.setStatus(AppointmentStatus.APPROVED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        notificationService.createAndSendSmsSafely(
                savedAppointment.getCustomer(),
                savedAppointment,
                NotificationType.APPOINTMENT_APPROVED,
                "Randevu onaylandi",
                "GlowBook randevunuz onaylandi: " + savedAppointment.getAppointmentDate() + " " + savedAppointment.getAppointmentTime(),
                savedAppointment.getPhone()
        );
        return savedAppointment;
    }

    @Transactional
    public Appointment complete(Integer appointmentId) {
        Appointment appointment = getById(appointmentId);
        ensureNotCancelled(appointment);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancel(Integer appointmentId, String cancellationReason) {
        Appointment appointment = getById(appointmentId);

        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            return appointment;
        }

        if (appointment.getCustomerPackage() != null) {
            customerPackageService.restoreSession(appointment.getCustomerPackage().getCustomerPackageId());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(cancellationReason);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        notificationService.createAndSendSmsSafely(
                savedAppointment.getCustomer(),
                savedAppointment,
                NotificationType.APPOINTMENT_CANCELLED,
                "Randevu iptal edildi",
                "GlowBook randevunuz iptal edildi: " + savedAppointment.getAppointmentDate() + " " + savedAppointment.getAppointmentTime(),
                savedAppointment.getPhone()
        );
        return savedAppointment;
    }

    @Transactional
    public Appointment updateTime(Integer appointmentId, Appointment request) {
        Appointment appointment = getById(appointmentId);
        ensureNotCancelled(appointment);

        Appointment availabilityRequest = Appointment.builder()
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .build();

        validateAvailability(
                appointment.getEmployee().getEmployeeId(),
                appointment.getService().getServiceId(),
                availabilityRequest
        );

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());

        return appointmentRepository.save(appointment);
    }

    private void validateAvailability(String employeeId, Integer serviceId, Appointment request) {
        if (!employeeServiceAssignmentService.employeeCanProvideService(employeeId, serviceId)) {
            throw new BusinessException("Employee cannot provide selected service");
        }

        if (holidayService.isHoliday(request.getAppointmentDate())) {
            throw new BusinessException("Appointments cannot be created on holidays");
        }

        if (employeeLeaveService.isEmployeeOnLeave(employeeId, request.getAppointmentDate())) {
            throw new BusinessException("Employee is on leave on selected date");
        }

        if (!workingHourService.isWorkingTime(request.getAppointmentDate().getDayOfWeek(), request.getAppointmentTime())) {
            throw new BusinessException("Selected time is outside working hours");
        }

        boolean occupied = appointmentRepository.existsByEmployeeEmployeeIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                employeeId,
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                BLOCKING_STATUSES
        );

        if (occupied) {
            throw new ConflictException("Selected appointment time is already occupied");
        }
    }

    private Customer resolveCustomer(Appointment request) {
        if (request.getCustomer() == null || request.getCustomer().getCustomerId() == null) {
            requireGuestInfo(request);
            return null;
        }

        Customer customer = customerService.getById(request.getCustomer().getCustomerId());

        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            request.setCustomerName(customer.getFirstName());
        }

        if (request.getCustomerSurname() == null || request.getCustomerSurname().isBlank()) {
            request.setCustomerSurname(customer.getLastName());
        }

        if (request.getPhone() == null || request.getPhone().isBlank()) {
            request.setPhone(customer.getPhone());
        }

        return customer;
    }

    private void requireGuestInfo(Appointment request) {
        if (isBlank(request.getCustomerName()) || isBlank(request.getCustomerSurname()) || isBlank(request.getPhone())) {
            throw new BusinessException("Customer name, surname and phone are required for guest appointments");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private CustomerPackage resolveAndUseCustomerPackage(Appointment request, Customer customer, Integer serviceId) {
        if (request.getCustomerPackage() == null || request.getCustomerPackage().getCustomerPackageId() == null) {
            return null;
        }

        if (customer == null) {
            throw new BusinessException("Customer is required when using a package");
        }

        return customerPackageService.useSession(
                request.getCustomerPackage().getCustomerPackageId(),
                customer.getCustomerId(),
                serviceId
        );
    }

    private void ensureNotCancelled(Appointment appointment) {
        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            throw new BusinessException("Cancelled appointment cannot be changed");
        }
    }
}
