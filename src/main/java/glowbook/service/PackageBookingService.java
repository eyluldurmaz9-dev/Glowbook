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
 * <p>The service <b>and sub-service</b> the appointment is for are <b>derived from the
 * package</b>, never taken from the caller: {@link ServicePackage#getCoveredOptions()} is
 * the sole authority on what a package may be booked for (see
 * docs/PACKAGE_SERVICE_COVERAGE.md) — never the package's display name or its broad {@link
 * ServicePackage#getService()} category, which usually has several sibling sub-services a
 * given package does not cover. Asking the customer to pick again would be redundant when
 * a package covers exactly one sub-service; only a genuinely multi-option package still
 * asks, and only among the options it actually covers.</p>
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
        ServiceOption option = resolveCoveredOption(servicePackage, command.optionId());
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
     * The package already fixes the service <b>and</b> which sub-service(s) it covers
     * (see {@link ServicePackage#getCoveredOptions()}). If it covers a single sub-service
     * we pick it silently; a genuinely multi-option package still requires a choice, but
     * only among its own covered options — never the wider service catalog. A mismatched
     * option (one that belongs to the same service but not to this specific package) is
     * rejected the same as one from an unrelated service.
     */
    private ServiceOption resolveCoveredOption(ServicePackage servicePackage, Integer requestedOptionId) {
        List<ServiceOption> covered = servicePackage.getCoveredOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.getActive()))
                .toList();
        if (covered.isEmpty()) {
            throw new BusinessException("Bu paket için uygun bir hizmet seçeneği bulunamadı.");
        }

        if (requestedOptionId != null) {
            return covered.stream()
                    .filter(option -> option.getOptionId().equals(requestedOptionId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Bu hizmet seçtiğin pakete dahil değil."));
        }

        if (covered.size() > 1) {
            throw new BusinessException("Bu paket birden fazla seçenek kapsıyor. Lütfen birini seç.");
        }
        return covered.getFirst();
    }
}
