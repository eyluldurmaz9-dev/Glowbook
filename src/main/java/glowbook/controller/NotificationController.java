package glowbook.controller;

import glowbook.dto.ApiResponse;
import glowbook.dto.DtoMapper;
import glowbook.dto.NotificationDtos;
import glowbook.entity.Notification;
import glowbook.security.AuthorizationSupport;
import glowbook.security.UserRole;
import glowbook.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthorizationSupport authorizationSupport;

    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<NotificationDtos.NotificationResponse>> getCustomerNotifications(
            @PathVariable Integer customerId,
            Authentication authentication
    ) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        return ApiResponse.success("Notifications listed", notificationService.getCustomerNotifications(customerId)
                .stream()
                .map(DtoMapper::toNotificationResponse)
                .toList());
    }

    @GetMapping("/customer/{customerId}/unread")
    public ApiResponse<List<NotificationDtos.NotificationResponse>> getUnreadCustomerNotifications(
            @PathVariable Integer customerId,
            Authentication authentication
    ) {
        authorizationSupport.assertCustomerCanAccess(customerId, authentication);
        return ApiResponse.success("Unread notifications listed", notificationService.getUnreadCustomerNotifications(customerId)
                .stream()
                .map(DtoMapper::toNotificationResponse)
                .toList());
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationDtos.NotificationResponse> markAsRead(
            @PathVariable Integer notificationId,
            Authentication authentication
    ) {
        Notification notification = notificationService.getById(notificationId);
        if (notification.getCustomer() != null) {
            authorizationSupport.assertCustomerCanAccess(notification.getCustomer().getCustomerId(), authentication);
        } else if (!authorizationSupport.hasAnyRole(authentication, UserRole.ADMIN, UserRole.EMPLOYEE)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return ApiResponse.success("Notification marked as read", DtoMapper.toNotificationResponse(notificationService.markAsRead(notificationId)));
    }
}
