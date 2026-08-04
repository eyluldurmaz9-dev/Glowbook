package glowbook.service;

import glowbook.dto.AuthDtos;
import glowbook.entity.Customer;
import glowbook.entity.Employee;
import glowbook.exception.BusinessException;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerService customerService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthDtos.AuthResponse registerCustomer(AuthDtos.RegisterCustomerRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .active(true)
                .build();

        Customer savedCustomer = customerService.create(customer);
        String token = jwtTokenService.generateToken(String.valueOf(savedCustomer.getCustomerId()), UserRole.CUSTOMER);
        String refresh = createRefreshTokenOrNull(String.valueOf(savedCustomer.getCustomerId()), UserRole.CUSTOMER);

        return new AuthDtos.AuthResponse(
            token,
            refresh,
            "Bearer",
            UserRole.CUSTOMER.name(),
            savedCustomer.getCustomerId(),
            null,
            savedCustomer.getFirstName() + " " + savedCustomer.getLastName()
        );
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserRole role = UserRole.valueOf(request.role().toUpperCase());
        return switch (role) {
            case CUSTOMER -> loginCustomer(request);
            case EMPLOYEE, ADMIN -> loginEmployee(request, role);
        };
    }

    private AuthDtos.AuthResponse loginCustomer(AuthDtos.LoginRequest request) {
        Customer customer = customerRepository.findByPhone(request.username())
                .orElseThrow(() -> new BusinessException("Invalid phone or password"));

        if (!Boolean.TRUE.equals(customer.getActive()) || !passwordMatches(request.password(), customer.getPassword())) {
            throw new BusinessException("Invalid phone or password");
        }

        String token = jwtTokenService.generateToken(String.valueOf(customer.getCustomerId()), UserRole.CUSTOMER);
        String refresh = createRefreshTokenOrNull(String.valueOf(customer.getCustomerId()), UserRole.CUSTOMER);
        return new AuthDtos.AuthResponse(
            token,
            refresh,
            "Bearer",
            UserRole.CUSTOMER.name(),
            customer.getCustomerId(),
            null,
            customer.getFirstName() + " " + customer.getLastName()
        );
    }

    private AuthDtos.AuthResponse loginEmployee(AuthDtos.LoginRequest request, UserRole requestedRole) {
        Employee employee = employeeRepository.findByEmployeeIdAndActiveTrue(request.username())
                .orElseThrow(() -> new BusinessException("Invalid employee id or password"));

        if (!passwordMatches(request.password(), employee.getPassword())) {
            throw new BusinessException("Invalid employee id or password");
        }

        boolean adminEmployee = "ADMIN".equalsIgnoreCase(employee.getEmployeeId());
        if (requestedRole == UserRole.ADMIN && !adminEmployee) {
            throw new BusinessException("Invalid employee id or password");
        }
        UserRole tokenRole = adminEmployee ? UserRole.ADMIN : UserRole.EMPLOYEE;
        String token = jwtTokenService.generateToken(employee.getEmployeeId(), tokenRole);
        String refresh = createRefreshTokenOrNull(employee.getEmployeeId(), tokenRole);

        return new AuthDtos.AuthResponse(
                token,
                refresh,
                "Bearer",
                tokenRole.name(),
                null,
                employee.getEmployeeId(),
                employee.getFirstName() + " " + employee.getLastName()
        );
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(String refreshToken) {
        var opt = refreshTokenService.findByToken(refreshToken);
        var tokenEntity = opt.orElseThrow(() -> new BusinessException("Invalid refresh token"));
        refreshTokenService.validate(tokenEntity);

        String subject = tokenEntity.getSubject();
        UserRole role = UserRole.valueOf(tokenEntity.getRole());
        Integer customerId = null;
        String employeeId = null;
        try {
            customerId = Integer.valueOf(subject);
        } catch (NumberFormatException ex) {
            employeeId = subject;
        }

        refreshTokenService.revoke(tokenEntity);
        var newRefresh = refreshTokenService.create(subject, role).getToken();

        String access = jwtTokenService.generateToken(subject, role);
        return new AuthDtos.AuthResponse(
                access,
                newRefresh,
                "Bearer",
                role.name(),
                customerId,
                employeeId,
                null
        );
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    private String createRefreshTokenOrNull(String subject, UserRole role) {
        try {
            return refreshTokenService.create(subject, role).getToken();
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
