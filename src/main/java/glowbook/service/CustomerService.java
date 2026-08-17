package glowbook.service;

import glowbook.entity.Customer;
import glowbook.exception.BusinessException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getById(Integer customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Müşteri bulunamadı."));
    }

    @Transactional
    public Customer getByIdForPackagePurchase(Integer customerId) {
        return customerRepository.findLockedByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Müşteri bulunamadı."));
    }

    public Customer getByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Bu telefon numarasıyla müşteri bulunamadı."));
    }

    @Transactional
    public Customer create(Customer customer) {
        validateUniquePhone(customer.getPhone(), null);
        validateUniqueEmail(customer.getEmail(), null);
        customer.setPassword(encodePasswordIfNeeded(customer.getPassword()));
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Integer customerId, Customer request) {
        Customer customer = getById(customerId);

        validateUniquePhone(request.getPhone(), customerId);
        validateUniqueEmail(request.getEmail(), customerId);

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        customer.setPassword(encodePasswordIfNeeded(request.getPassword()));
        customer.setEmail(request.getEmail());
        customer.setActive(request.getActive());

        return customerRepository.save(customer);
    }

    @Transactional
    public Customer deactivate(Integer customerId) {
        Customer customer = getById(customerId);
        customer.setActive(false);
        return customerRepository.save(customer);
    }

    private void validateUniquePhone(String phone, Integer currentCustomerId) {
        customerRepository.findByPhone(phone)
                .filter(customer -> !customer.getCustomerId().equals(currentCustomerId))
                .ifPresent(customer -> {
                    throw new BusinessException("Bu telefon numarasıyla zaten bir hesap var.");
                });
    }

    private void validateUniqueEmail(String email, Integer currentCustomerId) {
        if (email == null || email.isBlank()) {
            return;
        }

        customerRepository.findByEmail(email)
                .filter(customer -> !customer.getCustomerId().equals(currentCustomerId))
                .ifPresent(customer -> {
                    throw new BusinessException("Bu e-posta adresiyle zaten bir hesap var.");
                });
    }

    private String encodePasswordIfNeeded(String password) {
        if (password == null || password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return password;
        }
        return passwordEncoder.encode(password);
    }
}
