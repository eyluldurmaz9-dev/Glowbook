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
import org.springframework.context.ApplicationEventPublisher;
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
    private final AppointmentTimeClassifier appointmentTimeClassifier;
    private final TurkishPhoneNumberService phoneNumberService;
    private final ApplicationEventPublisher eventPublisher;

    public Appointment getById(Integer appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Randevu bulunamadı."));
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
        return appointmentRepository.findByCustomerCustomerIdOrderByAppointmentDateAscAppointmentTimeAsc(customerId)
                .stream().filter(appointmentTimeClassifier::isUpcoming).toList();
    }

    public List<Appointment> getPastCustomerAppointments(Integer customerId, LocalDate beforeDate) {
        return appointmentRepository.findByCustomerCustomerIdOrderByAppointmentDateAscAppointmentTimeAsc(customerId)
                .stream().filter(appointmentTimeClassifier::isPast)
                .sorted((left, right) -> {
                    int date = right.getAppointmentDate().compareTo(left.getAppointmentDate());
                    return date != 0 ? date : right.getAppointmentTime().compareTo(left.getAppointmentTime());
                }).toList();
    }

    @Transactional
    public Appointment create(Appointment request) {
        Employee employee = employeeManagementService.getActiveByIdForBooking(request.getEmployee().getEmployeeId());
        glowbook.entity.Service service = serviceCatalogService.getActiveById(request.getService().getServiceId());
        ServiceOption option = serviceOptionService.getActiveByService(service.getServiceId(), request.getServiceOption().getOptionId());

        assertEmployeeQualification(employee.getEmployeeId(), service.getServiceId(), option.getOptionId());
        appointmentAlgorithmService.validateSlot(
                employee.getEmployeeId(),
                service.getServiceId(),
                option.getOptionId(),
                request.getAppointmentDate(),
                request.getAppointmentTime()
        );
        validateAvailability(employee.getEmployeeId(), request);
        Customer customer = resolveCustomer(request);
        request.setPhone(phoneNumberService.normalize(request.getPhone()));
        CustomerPackage customerPackage =
                resolveAndUseCustomerPackage(request, customer, service.getServiceId(), option.getOptionId());

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
        if (customerPackage != null) {
            // The saved appointment now backs exactly one session; recompute the stored counter.
            customerPackageService.synchronize(customerPackage);
        }
        publishSms(appointment, NotificationType.APPOINTMENT_CREATED, "Randevu oluşturuldu",
                appointmentMessage("Randevunuz oluşturuldu", appointment));
        return appointment;
    }

    @Transactional
    public Appointment approve(Integer appointmentId) {
        Appointment appointment = getById(appointmentId);
        ensureNotCancelled(appointment);
        employeeManagementService.getActiveById(appointment.getEmployee().getEmployeeId());
        assertEmployeeQualification(
                appointment.getEmployee().getEmployeeId(),
                appointment.getService().getServiceId(),
                appointment.getServiceOption().getOptionId());
        appointment.setStatus(AppointmentStatus.APPROVED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        publishSms(savedAppointment, NotificationType.APPOINTMENT_APPROVED, "Randevu onaylandı",
                appointmentMessage("Randevunuz onaylandı", savedAppointment));
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

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(cancellationReason);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        if (savedAppointment.getCustomerPackage() != null) {
            // Cancelling releases the session; the recomputation cannot restore it twice.
            customerPackageService.synchronize(savedAppointment.getCustomerPackage());
        }
        publishSms(savedAppointment, NotificationType.APPOINTMENT_CANCELLED, "Randevu iptal edildi",
                appointmentMessage("Randevunuz iptal edildi", savedAppointment));
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
                availabilityRequest
        );

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        publishSms(savedAppointment, NotificationType.APPOINTMENT_UPDATED, "Randevu değiştirildi",
                appointmentMessage("Randevunuz değiştirildi", savedAppointment));
        return savedAppointment;
    }

    private void assertEmployeeQualification(String employeeId, Integer serviceId, Integer optionId) {
        if (!employeeServiceAssignmentService.employeeCanProvideServiceOption(employeeId, serviceId, optionId)) {
            throw new BusinessException("Seçilen personel bu hizmeti vermiyor.");
        }
    }

    private void validateAvailability(String employeeId, Appointment request) {
        appointmentAlgorithmService.validateAppointmentTime(
                request.getAppointmentDate(), request.getAppointmentTime());
        if (holidayService.isHoliday(request.getAppointmentDate())) {
            throw new BusinessException("Bu tarihte randevu oluşturulamaz, salon tatilde.");
        }

        if (employeeLeaveService.isEmployeeOnLeave(employeeId, request.getAppointmentDate())) {
            throw new BusinessException("Seçilen personel bu tarihte izinli.");
        }

        if (!workingHourService.isWorkingTime(request.getAppointmentDate().getDayOfWeek(), request.getAppointmentTime())) {
            throw new BusinessException("Seçilen saat çalışma saatleri dışında.");
        }

        boolean occupied = appointmentRepository.existsByEmployeeEmployeeIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                employeeId,
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                BLOCKING_STATUSES
        );

        if (occupied) {
            throw new ConflictException("Bu saat başka bir randevu tarafından dolu. Lütfen başka bir saat seç.");
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
            throw new BusinessException("Misafir randevusu için ad, soyad ve telefon numarası gereklidir.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private CustomerPackage resolveAndUseCustomerPackage(
            Appointment request, Customer customer, Integer serviceId, Integer optionId) {
        if (request.getCustomerPackage() == null || request.getCustomerPackage().getCustomerPackageId() == null) {
            return null;
        }

        if (customer == null) {
            throw new BusinessException("Paket kullanmak için üye girişi yapmalısın.");
        }

        return customerPackageService.reserveSession(
                request.getCustomerPackage().getCustomerPackageId(),
                customer.getCustomerId(),
                serviceId,
                optionId
        );
    }

    private void ensureNotCancelled(Appointment appointment) {
        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            throw new BusinessException("İptal edilmiş bir randevu değiştirilemez.");
        }
    }

    private void publishSms(Appointment appointment, NotificationType type, String title, String message) {
        eventPublisher.publishEvent(new AppointmentSmsEvent(
                appointment.getAppointmentId(), type, title, message, appointment.getPhone()));
    }

    private String appointmentMessage(String action, Appointment appointment) {
        String serviceName = appointment.getService() == null ? "" : ", " + appointment.getService().getServiceName();
        String employeeName = appointment.getEmployee() == null ? "" : ", "
                + appointment.getEmployee().getFirstName() + " " + appointment.getEmployee().getLastName();
        return "GlowBook: " + action + ". " + appointment.getAppointmentDate() + " "
                + appointment.getAppointmentTime() + serviceName + employeeName + ". Görüşmek üzere.";
    }
}
