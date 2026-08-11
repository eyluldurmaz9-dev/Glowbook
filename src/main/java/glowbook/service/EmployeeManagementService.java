package glowbook.service;

import glowbook.entity.Employee;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeManagementService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc();
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getById(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    public Employee getActiveById(String employeeId) {
        return employeeRepository.findByEmployeeIdAndActiveTrue(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee not found: " + employeeId));
    }

    @Transactional
    public Employee getActiveByIdForBooking(String employeeId) {
        return employeeRepository.findLockedByEmployeeIdAndActiveTrue(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee not found: " + employeeId));
    }

    @Transactional
    public Employee create(Employee employee) {
        employee.setPassword(encodePasswordIfNeeded(employee.getPassword()));
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(String employeeId, Employee request) {
        Employee employee = getById(employeeId);

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPassword(encodePasswordIfNeeded(request.getPassword()));
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setActive(request.getActive());

        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee deactivate(String employeeId) {
        Employee employee = getById(employeeId);
        employee.setActive(false);
        return employeeRepository.save(employee);
    }

    private String encodePasswordIfNeeded(String password) {
        if (password == null || password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return password;
        }
        return passwordEncoder.encode(password);
    }
}
