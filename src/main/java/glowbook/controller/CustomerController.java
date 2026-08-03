package glowbook.controller;

import glowbook.dto.ApiResponse;
import glowbook.dto.CustomerDtos;
import glowbook.dto.DtoMapper;
import glowbook.entity.Customer;
import glowbook.security.AuthorizationSupport;
import glowbook.service.CustomerPackageService;
import glowbook.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerPackageService customerPackageService;
    private final AuthorizationSupport authorizationSupport;

    @GetMapping("/admin/customers")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<List<CustomerDtos.CustomerResponse>> getCustomers() {
        return ApiResponse.success("Customers listed", customerService.getAllCustomers()
                .stream()
                .map(DtoMapper::toCustomerResponse)
                .toList());
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ApiResponse<CustomerDtos.CustomerResponse> getCustomer(@PathVariable Integer customerId, Authentication authentication) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        return ApiResponse.success("Customer found", DtoMapper.toCustomerResponse(customerService.getById(customerId)));
    }

    @PutMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ApiResponse<CustomerDtos.CustomerResponse> updateCustomer(
            @PathVariable Integer customerId,
            @Valid @RequestBody CustomerDtos.CustomerRequest request,
            Authentication authentication
    ) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .password(request.password())
                .email(request.email())
                .active(request.active())
                .build();
        return ApiResponse.success("Customer updated", DtoMapper.toCustomerResponse(customerService.update(customerId, customer)));
    }

    @PostMapping("/customers/{customerId}/packages/{packageId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ApiResponse<CustomerDtos.CustomerPackageResponse> purchasePackage(
            @PathVariable Integer customerId,
            @PathVariable Integer packageId,
            Authentication authentication
    ) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        return ApiResponse.success("Package purchased", DtoMapper.toCustomerPackageResponse(customerPackageService.purchase(customerId, packageId)));
    }

    @GetMapping("/customers/{customerId}/packages")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ApiResponse<List<CustomerDtos.CustomerPackageResponse>> getCustomerPackages(@PathVariable Integer customerId, Authentication authentication) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        return ApiResponse.success("Customer packages listed", customerPackageService.getActivePackagesByCustomer(customerId)
                .stream()
                .map(DtoMapper::toCustomerPackageResponse)
                .toList());
    }

}
