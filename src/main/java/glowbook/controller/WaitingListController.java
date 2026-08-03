package glowbook.controller;

import glowbook.dto.ApiResponse;
import glowbook.dto.DtoMapper;
import glowbook.dto.WaitingListDtos;
import glowbook.entity.Customer;
import glowbook.entity.ServiceOption;
import glowbook.entity.WaitingList;
import glowbook.security.AuthorizationSupport;
import glowbook.security.UserRole;
import glowbook.service.WaitingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waiting-list")
@RequiredArgsConstructor
public class WaitingListController {

    private final WaitingListService waitingListService;
    private final AuthorizationSupport authorizationSupport;

    @PostMapping
    public ApiResponse<WaitingListDtos.WaitingListResponse> create(@Valid @RequestBody WaitingListDtos.WaitingListRequest request) {
        return ApiResponse.success("Waiting list record created", DtoMapper.toWaitingListResponse(waitingListService.create(toWaitingList(request))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<List<WaitingListDtos.WaitingListResponse>> getActiveWaitingList() {
        return ApiResponse.success("Waiting list records listed", waitingListService.getActiveWaitingList()
                .stream()
                .map(DtoMapper::toWaitingListResponse)
                .toList());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ApiResponse<List<WaitingListDtos.WaitingListResponse>> getCustomerWaitingList(
            @PathVariable Integer customerId,
            Authentication authentication
    ) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        return ApiResponse.success("Customer waiting list records listed", waitingListService.getActiveByCustomer(customerId)
                .stream()
                .map(DtoMapper::toWaitingListResponse)
                .toList());
    }

    @PatchMapping("/{waitingListId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WaitingListDtos.WaitingListResponse> cancel(
            @PathVariable Integer waitingListId,
            Authentication authentication
    ) {
        assertCanAccessWaitingList(waitingListService.getById(waitingListId), authentication);
        return ApiResponse.success("Waiting list record cancelled", DtoMapper.toWaitingListResponse(waitingListService.cancel(waitingListId)));
    }

    @PatchMapping("/{waitingListId}/converted")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<WaitingListDtos.WaitingListResponse> markConverted(@PathVariable Integer waitingListId) {
        return ApiResponse.success("Waiting list record converted", DtoMapper.toWaitingListResponse(waitingListService.markConverted(waitingListId)));
    }

    private WaitingList toWaitingList(WaitingListDtos.WaitingListRequest request) {
        return WaitingList.builder()
                .customer(request.customerId() == null ? null : Customer.builder().customerId(request.customerId()).build())
                .service(glowbook.entity.Service.builder().serviceId(request.serviceId()).build())
                .serviceOption(ServiceOption.builder().optionId(request.optionId()).build())
                .customerName(request.customerName())
                .customerSurname(request.customerSurname())
                .phone(request.phone())
                .preferredDate(request.preferredDate())
                .preferredStartTime(request.preferredStartTime())
                .preferredEndTime(request.preferredEndTime())
                .build();
    }

    private void assertCanAccessWaitingList(WaitingList waitingList, Authentication authentication) {
        if (waitingList.getCustomer() != null) {
            authorizationSupport.assertCustomerCanAccess(waitingList.getCustomer().getCustomerId(), authentication);
            return;
        }
        if (!authorizationSupport.hasAnyRole(authentication, UserRole.ADMIN, UserRole.EMPLOYEE)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
    }
}
