package glowbook.service;

import glowbook.dto.AuthDtos;
import glowbook.entity.Employee;
import glowbook.exception.BusinessException;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void regularEmployeeCannotEscalateToAdminRole() {
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Employee employee = Employee.builder()
                .employeeId("EMP-1")
                .firstName("Elif")
                .lastName("Yilmaz")
                .password("encoded")
                .active(true)
                .build();

        when(employeeRepository.findByEmployeeIdAndActiveTrue("EMP-1"))
                .thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        AuthService authService = new AuthService(
                mock(CustomerRepository.class),
                employeeRepository,
                null,
                null,
                null,
                passwordEncoder
        );

        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(
                "EMP-1",
                "secret",
                "ADMIN"
        );

        assertThrows(BusinessException.class, () -> authService.login(request));
    }
}
