package glowbook.service;

import glowbook.entity.Customer;
import glowbook.entity.CustomerPackage;
import glowbook.entity.ServicePackage;
import glowbook.exception.BusinessException;
import glowbook.exception.ConflictException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.CustomerPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerPackageService {

    private final CustomerPackageRepository customerPackageRepository;
    private final CustomerService customerService;
    private final ServicePackageService servicePackageService;
    private final PackageSessionAccountingService packageSessionAccountingService;
    private final Clock businessClock;

    public List<CustomerPackage> getActivePackagesByCustomer(Integer customerId) {
        LocalDate today = LocalDate.now(businessClock);
        return customerPackageRepository.findByCustomerCustomerIdAndActiveTrueOrderByPurchaseDateDesc(customerId)
                .stream().filter(item -> item.getValidUntil() == null || !item.getValidUntil().isBefore(today)).toList();
    }

    public CustomerPackage getById(Integer customerPackageId) {
        return customerPackageRepository.findById(customerPackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer package not found: " + customerPackageId));
    }

    public CustomerPackage getActiveByCustomer(Integer customerPackageId, Integer customerId) {
        return customerPackageRepository.findByCustomerPackageIdAndCustomerCustomerIdAndActiveTrue(customerPackageId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Active customer package not found: " + customerPackageId));
    }

    public PackageSessionAccounting accountingOf(CustomerPackage customerPackage) {
        return packageSessionAccountingService.calculate(customerPackage);
    }

    @Transactional
    public CustomerPackage purchase(Integer customerId, Integer packageId) {
        Customer customer = customerService.getByIdForPackagePurchase(customerId);
        ServicePackage servicePackage = servicePackageService.getActiveById(packageId);

        if (customerPackageRepository.existsByCustomerCustomerIdAndServicePackagePackageIdAndActiveTrue(customerId, packageId)) {
            throw new ConflictException("Bu paketin aktif bir kopyasına zaten sahipsin.");
        }

        LocalDate today = LocalDate.now(businessClock);
        CustomerPackage customerPackage = CustomerPackage.builder()
                .customer(customer)
                .servicePackage(servicePackage)
                .remainingSession(servicePackage.getTotalSession())
                .purchasePrice(servicePackage.getPrice())
                .purchaseDate(today)
                .validUntil(today.plusDays(effectiveValidityDays(servicePackage)))
                .active(true)
                .build();

        return customerPackageRepository.save(customerPackage);
    }

    @Transactional
    public CustomerPackage update(Integer customerPackageId, CustomerPackage request) {
        CustomerPackage customerPackage = getById(customerPackageId);

        customerPackage.setRemainingSession(request.getRemainingSession());
        customerPackage.setPurchasePrice(request.getPurchasePrice());
        customerPackage.setPurchaseDate(request.getPurchaseDate());
        customerPackage.setValidUntil(request.getValidUntil());
        customerPackage.setActive(request.getActive());

        return customerPackageRepository.save(customerPackage);
    }

    /**
     * Validates that the package may back one more appointment for the given service
     * <b>and specific sub-service</b> — {@link glowbook.entity.ServicePackage#getCoveredOptions()}
     * is the sole authority here (see docs/PACKAGE_SERVICE_COVERAGE.md); a service-level
     * match alone is not enough, since most services have sibling sub-services a given
     * package was never sold to cover.
     *
     * <p>Deliberately does <b>not</b> decrement anything: the session is consumed by the
     * existence of the appointment row itself. {@link #synchronize(CustomerPackage)} writes
     * the derived remaining value once the appointment has been persisted, which makes
     * double subtraction and double restoration structurally impossible.</p>
     */
    @Transactional
    public CustomerPackage reserveSession(Integer customerPackageId, Integer customerId, Integer serviceId, Integer optionId) {
        CustomerPackage customerPackage = getActiveByCustomer(customerPackageId, customerId);

        if (!customerPackage.getServicePackage().getService().getServiceId().equals(serviceId)) {
            throw new BusinessException("Seçilen paket bu hizmet için geçerli değil.");
        }

        boolean optionCovered = customerPackage.getServicePackage().getCoveredOptions().stream()
                .anyMatch(option -> option.getOptionId().equals(optionId));
        if (!optionCovered) {
            throw new BusinessException("Bu hizmet seçtiğin pakete dahil değil.");
        }

        if (customerPackage.getValidUntil() != null
                && customerPackage.getValidUntil().isBefore(LocalDate.now(businessClock))) {
            customerPackage.setActive(false);
            customerPackageRepository.save(customerPackage);
            throw new BusinessException("Paketinin geçerlilik süresi dolmuş.");
        }

        if (!packageSessionAccountingService.calculate(customerPackage).hasBookableSession()) {
            throw new BusinessException("Paketinde planlanabilir seans kalmadı.");
        }

        return customerPackage;
    }

    /**
     * Recomputes and stores the package's remaining session count from its appointments.
     */
    @Transactional
    public PackageSessionAccounting synchronize(CustomerPackage customerPackage) {
        return packageSessionAccountingService.synchronize(customerPackage);
    }

    @Transactional
    public CustomerPackage deactivate(Integer customerPackageId) {
        CustomerPackage customerPackage = getById(customerPackageId);
        customerPackage.setActive(false);
        return customerPackageRepository.save(customerPackage);
    }

    private long effectiveValidityDays(ServicePackage servicePackage) {
        return servicePackage.getValidityDays() == null ? 365 : servicePackage.getValidityDays();
    }
}
