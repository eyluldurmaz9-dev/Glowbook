package glowbook.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationSupport {

    public boolean hasRole(Authentication authentication, UserRole role) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> ("ROLE_" + role.name()).equals(authority.getAuthority()));
    }

    public boolean hasAnyRole(Authentication authentication, UserRole... roles) {
        for (UserRole role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }

    public void assertCustomerCanAccess(Integer customerId, Authentication authentication) {
        if (hasAnyRole(authentication, UserRole.ADMIN, UserRole.EMPLOYEE)) {
            return;
        }
        if (hasRole(authentication, UserRole.CUSTOMER)
                && customerId != null
                && String.valueOf(customerId).equals(authentication.getName())) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    public void assertEmployeeCanAccess(String employeeId, Authentication authentication) {
        if (hasRole(authentication, UserRole.ADMIN)) {
            return;
        }
        if (hasRole(authentication, UserRole.EMPLOYEE)
                && employeeId != null
                && employeeId.equals(authentication.getName())) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }
}
