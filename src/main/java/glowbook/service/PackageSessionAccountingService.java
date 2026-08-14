package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.CustomerPackage;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single authoritative place where package session usage is calculated.
 *
 * <p>Nothing else in the codebase is allowed to increment or decrement a session
 * counter on its own; callers ask this service and, when appointments change,
 * ask it to write the derived remaining value back onto the package row.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackageSessionAccountingService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerPackageRepository customerPackageRepository;
    private final AppointmentTimeClassifier appointmentTimeClassifier;

    public PackageSessionAccounting calculate(CustomerPackage customerPackage) {
        if (customerPackage == null || customerPackage.getCustomerPackageId() == null) {
            return PackageSessionAccounting.of(0, 0, 0);
        }
        return calculate(
                customerPackage,
                appointmentRepository.findByCustomerPackageCustomerPackageId(customerPackage.getCustomerPackageId())
        );
    }

    /**
     * Batch variant so listing a customer's packages stays a single appointment query.
     */
    public Map<Integer, PackageSessionAccounting> calculateAll(Collection<CustomerPackage> customerPackages) {
        Map<Integer, PackageSessionAccounting> result = new HashMap<>();
        if (customerPackages == null || customerPackages.isEmpty()) {
            return result;
        }

        List<Integer> ids = customerPackages.stream()
                .map(CustomerPackage::getCustomerPackageId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Integer, List<Appointment>> byPackage = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Appointment appointment : appointmentRepository.findByCustomerPackageCustomerPackageIdIn(ids)) {
                byPackage.computeIfAbsent(appointment.getCustomerPackage().getCustomerPackageId(),
                        key -> new java.util.ArrayList<>()).add(appointment);
            }
        }

        for (CustomerPackage customerPackage : customerPackages) {
            Integer id = customerPackage.getCustomerPackageId();
            if (id == null) {
                continue;
            }
            result.put(id, calculate(customerPackage, byPackage.getOrDefault(id, List.of())));
        }
        return result;
    }

    /**
     * Recomputes the accounting and persists {@code remainingSession} so the stored row
     * always agrees with the appointments. Called after any appointment create/cancel
     * that touches a package.
     */
    @Transactional
    public PackageSessionAccounting synchronize(CustomerPackage customerPackage) {
        if (customerPackage == null || customerPackage.getCustomerPackageId() == null) {
            return PackageSessionAccounting.of(0, 0, 0);
        }

        CustomerPackage managed = customerPackageRepository.findById(customerPackage.getCustomerPackageId())
                .orElse(customerPackage);
        PackageSessionAccounting accounting = calculate(managed);

        managed.setRemainingSession(accounting.remaining());
        if (accounting.remaining() > 0 && !Boolean.TRUE.equals(managed.getActive())) {
            managed.setActive(true);
        }
        customerPackageRepository.save(managed);
        return accounting;
    }

    private PackageSessionAccounting calculate(CustomerPackage customerPackage, List<Appointment> appointments) {
        int total = totalSessionsOf(customerPackage);
        int used = 0;
        int scheduled = 0;

        for (Appointment appointment : appointments) {
            if (appointmentTimeClassifier.isCancelled(appointment)) {
                continue;
            }
            if (appointmentTimeClassifier.hasStarted(appointment)) {
                used++;
            } else {
                scheduled++;
            }
        }

        return PackageSessionAccounting.of(total, used, scheduled);
    }

    private int totalSessionsOf(CustomerPackage customerPackage) {
        if (customerPackage.getServicePackage() == null
                || customerPackage.getServicePackage().getTotalSession() == null) {
            return customerPackage.getRemainingSession() == null ? 0 : customerPackage.getRemainingSession();
        }
        return customerPackage.getServicePackage().getTotalSession();
    }
}
