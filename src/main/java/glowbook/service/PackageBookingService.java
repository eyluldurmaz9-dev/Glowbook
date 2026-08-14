package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.Customer;
import glowbook.entity.CustomerPackage;
import glowbook.entity.Employee;
import glowbook.entity.ServiceOption;
import glowbook.entity.ServicePackage;
import glowbook.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Buys a package and books its first appointment as one atomic operation.
 *
 * <p>The service the appointment is for is <b>derived from the package</b>, never taken
 * from the caller: a package always belongs to exactly one service, so asking the
 * customer to pick the service again would be redundant. Only the covered sub-service
 * (option) may still be required, and only when the service exposes more than one and
 * the customer has not picked one yet.</p>
 *
 * <p>Everything runs inside a single transaction: if slot validation or appointment
 * creation fails, the freshly created customer package is rolled back with it, so a
 * half-finished purchase can never be left behind.</p>
 */
@Service
@RequiredArgsConstructor
public class PackageBookingService {

    private final CustomerPackageService customerPackageService;
    private final ServicePackageService servicePackageService;
    private final ServiceOptionService serviceOptionService;
    private final CustomerService customerService;
    private final AppointmentService appointmentService;
    private final PackageSessionAccountingService packageSessionAccountingService;

    public record PackageBookingCommand(
            Integer customerId,
            Integer packageId,
            String employeeId,
            Integer optionId,
            LocalDate appointmentDate,
            LocalTime appointmentTime
    ) {
    }

    public record PackageBookingOutcome(
            CustomerPackage customerPackage,
            Appointment appointment,
            PackageSessionAccounting accounting
    ) {
    }

    @Transactional
    public PackageBookingOutcome purchaseAndBookFirstAppointment(PackageBookingCommand command) {
        ServicePackage servicePackage = servicePackageService.getActiveById(command.packageId());
        Integer serviceId = servicePackage.getService().getServiceId();
        ServiceOption option = resolveCoveredOption(serviceId, command.optionId());
        Customer customer = customerService.getByIdForPackagePurchase(command.customerId());

        CustomerPackage customerPackage = customerPackageService.purchase(command.customerId(), command.packageId());

        Appointment request = Appointment.builder()
                .customer(Customer.builder().customerId(customer.getCustomerId()).build())
                .customerPackage(CustomerPackage.builder()
                        .customerPackageId(customerPackage.getCustomerPackageId())
                        .build())
                .employee(Employee.builder().employeeId(command.employeeId()).build())
                .service(glowbook.entity.Service.builder().serviceId(serviceId).build())
                .serviceOption(ServiceOption.builder().optionId(option.getOptionId()).build())
                .appointmentDate(command.appointmentDate())
                .appointmentTime(command.appointmentTime())
                .build();

        // Any failure below (unavailable slot, past time, unqualified employee) rolls the purchase back.
        Appointment appointment = appointmentService.create(request);

        return new PackageBookingOutcome(
                customerPackage,
                appointment,
                packageSessionAccountingService.calculate(customerPackage)
        );
    }

    /**
     * The package already fixes the service. If it covers a single sub-service we pick it
     * silently; only a genuinely ambiguous choice is pushed back to the customer.
     */
    private ServiceOption resolveCoveredOption(Integer serviceId, Integer requestedOptionId) {
        if (requestedOptionId != null) {
            return serviceOptionService.getActiveByService(serviceId, requestedOptionId);
        }

        List<ServiceOption> covered = serviceOptionService.getActiveOptionsByService(serviceId);
        if (covered.isEmpty()) {
            throw new BusinessException("Bu paket için uygun bir hizmet seçeneği bulunamadı.");
        }
        if (covered.size() > 1) {
            throw new BusinessException("Bu paket birden fazla seçenek kapsıyor. Lütfen birini seç.");
        }
        return covered.getFirst();
    }
}
