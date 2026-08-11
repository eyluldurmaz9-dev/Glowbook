package glowbook.config;

import glowbook.entity.Customer;
import glowbook.entity.Employee;
import glowbook.entity.EmployeeService;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.ServiceRepository;
import glowbook.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-users.enabled", havingValue = "true")
public class DemoUserConfig {

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeServiceRepository employeeServiceRepository;
    private final ServiceRepository serviceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-users.admin-password}")
    private String adminPassword;
    @Value("${app.demo-users.employee-password}")
    private String employeePassword;
    @Value("${app.demo-users.customer-password}")
    private String customerPassword;

    @Bean
    @Order(100)
    ApplicationRunner seedDemoUsers() {
        return args -> createDemoUsers();
    }

    @Transactional
    void createDemoUsers() {
        requirePassword(adminPassword, "DEMO_ADMIN_PASSWORD");
        requirePassword(employeePassword, "DEMO_EMPLOYEE_PASSWORD");
        requirePassword(customerPassword, "DEMO_CUSTOMER_PASSWORD");

        Employee admin = upsertEmployee("DEMOADMIN", "Demo", "Admin", "admin@glowbook.test", adminPassword, UserRole.ADMIN);
        Employee employee = upsertEmployee("DEMOEMP", "Demo", "Employee", "employee@glowbook.test", employeePassword, UserRole.EMPLOYEE);
        upsertCustomer();

        serviceRepository.findAll().stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .forEach(service -> {
                    assign(admin, service);
                    assign(employee, service);
                });
    }

    private Employee upsertEmployee(String id, String firstName, String lastName, String email, String password, UserRole role) {
        Employee employee = employeeRepository.findById(id).orElseGet(Employee::new);
        employee.setEmployeeId(id);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPhone(null);
        employee.setPassword(passwordEncoder.encode(password));
        employee.setActive(true);
        employee.setRole(role);
        return employeeRepository.save(employee);
    }

    private void upsertCustomer() {
        Customer customer = customerRepository.findByEmail("customer@glowbook.test").orElseGet(Customer::new);
        customer.setFirstName("Demo");
        customer.setLastName("Customer");
        customer.setEmail("customer@glowbook.test");
        customer.setPhone("05550000011");
        customer.setPassword(passwordEncoder.encode(customerPassword));
        customer.setActive(true);
        customerRepository.save(customer);
    }

    private void assign(Employee employee, glowbook.entity.Service service) {
        if (!employeeServiceRepository.existsByEmployeeEmployeeIdAndServiceServiceId(employee.getEmployeeId(), service.getServiceId())) {
            employeeServiceRepository.save(EmployeeService.builder().employee(employee).service(service).build());
        }
    }

    private void requirePassword(String password, String environmentName) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(environmentName + " is required when ENABLE_DEMO_USERS=true");
        }
    }
}
