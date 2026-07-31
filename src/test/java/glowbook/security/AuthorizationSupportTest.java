package glowbook.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationSupportTest {

    private final AuthorizationSupport authorizationSupport = new AuthorizationSupport();

    @Test
    void customerCanAccessOnlyOwnResource() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "12",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        assertDoesNotThrow(() -> authorizationSupport.assertCustomerCanAccess(12, authentication));
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationSupport.assertCustomerCanAccess(13, authentication)
        );
    }

    @Test
    void adminCanAccessCustomerResource() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "ADMIN",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertDoesNotThrow(() -> authorizationSupport.assertCustomerCanAccess(12, authentication));
    }
}
