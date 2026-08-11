package glowbook.controller;

import glowbook.entity.Customer;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.ServicePackageRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PackagePurchaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CustomerRepository customerRepository;
    @Autowired ServicePackageRepository servicePackageRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtTokenService;

    @Test
    void purchaseCreatesCustomerPackageWithoutAppointmentAndRejectsDuplicate() throws Exception {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Package").lastName("Buyer")
                .phone("05551234569").email("package-buyer@glowbook.test")
                .password(passwordEncoder.encode("test-password")).active(true).build());
        Integer packageId = servicePackageRepository.findByActiveTrueOrderByPackageNameAsc()
                .getFirst().getPackageId();
        long appointmentsBefore = appointmentRepository.count();
        String token = jwtTokenService.generateToken(customer.getCustomerId().toString(), UserRole.CUSTOMER);
        String path = "/api/customers/" + customer.getCustomerId() + "/packages/" + packageId;

        mockMvc.perform(post(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.packageId").value(packageId))
                .andExpect(jsonPath("$.data.remainingSession").isNumber())
                .andExpect(jsonPath("$.data.validUntil").isString());
        org.assertj.core.api.Assertions.assertThat(appointmentRepository.count()).isEqualTo(appointmentsBefore);

        mockMvc.perform(post(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        org.assertj.core.api.Assertions.assertThat(appointmentRepository.count()).isEqualTo(appointmentsBefore);
    }
}
