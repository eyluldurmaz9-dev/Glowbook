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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerPackageService {

    private final CustomerPackageRepository customerPackageRepository;
    private final CustomerService customerService;
    private final ServicePackageService servicePackageService;

    public List<CustomerPackage> getActivePackagesByCustomer(Integer customerId) {
        return customerPackageRepository.findByCustomerCustomerIdAndActiveTrueOrderByPurchaseDateDesc(customerId)
                .stream().filter(item -> item.getValidUntil() == null || !item.getValidUntil().isBefore(LocalDate.now())).toList();
    }

    public CustomerPackage getById(Integer customerPackageId) {
        return customerPackageRepository.findById(customerPackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer package not found: " + customerPackageId));
    }

    public CustomerPackage getActiveByCustomer(Integer customerPackageId, Integer customerId) {
        return customerPackageRepository.findByCustomerPackageIdAndCustomerCustomerIdAndActiveTrue(customerPackageId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Active customer package not found: " + customerPackageId));
    }

    @Transactional
    public CustomerPackage purchase(Integer customerId, Integer packageId) {
        Customer customer = customerService.getByIdForPackagePurchase(customerId);
        ServicePackage servicePackage = servicePackageService.getActiveById(packageId);

        if (customerPackageRepository.existsByCustomerCustomerIdAndServicePackagePackageIdAndActiveTrue(customerId, packageId)) {
            throw new ConflictException("Customer already owns an active copy of this package");
        }

        CustomerPackage customerPackage = CustomerPackage.builder()
                .customer(customer)
                .servicePackage(servicePackage)
                .remainingSession(servicePackage.getTotalSession())
                .purchasePrice(servicePackage.getPrice())
                .purchaseDate(LocalDate.now())
                .validUntil(LocalDate.now().plusDays(servicePackage.getValidityDays()))
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

    @Transactional
    public CustomerPackage useSession(Integer customerPackageId, Integer customerId, Integer serviceId) {
        CustomerPackage customerPackage = getActiveByCustomer(customerPackageId, customerId);

        if (!customerPackage.getServicePackage().getService().getServiceId().equals(serviceId)) {
            throw new BusinessException("Customer package is not valid for selected service");
        }

        if (customerPackage.getRemainingSession() <= 0) {
            throw new BusinessException("Customer package has no remaining session");
        }

        if (customerPackage.getValidUntil() != null && customerPackage.getValidUntil().isBefore(LocalDate.now())) {
            customerPackage.setActive(false);
            customerPackageRepository.save(customerPackage);
            throw new BusinessException("Customer package has expired");
        }

        customerPackage.setRemainingSession(customerPackage.getRemainingSession() - 1);
        if (customerPackage.getRemainingSession() == 0) {
            customerPackage.setActive(false);
        }

        return customerPackageRepository.save(customerPackage);
    }

    @Transactional
    public CustomerPackage restoreSession(Integer customerPackageId) {
        CustomerPackage customerPackage = getById(customerPackageId);
        customerPackage.setRemainingSession(customerPackage.getRemainingSession() + 1);
        customerPackage.setActive(true);
        return customerPackageRepository.save(customerPackage);
    }

    @Transactional
    public CustomerPackage deactivate(Integer customerPackageId) {
        CustomerPackage customerPackage = getById(customerPackageId);
        customerPackage.setActive(false);
        return customerPackageRepository.save(customerPackage);
    }
}
