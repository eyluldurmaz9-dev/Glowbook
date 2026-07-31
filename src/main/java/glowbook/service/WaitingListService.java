package glowbook.service;

import glowbook.entity.*;
import glowbook.exception.BusinessException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.WaitingListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaitingListService {

    private final WaitingListRepository waitingListRepository;
    private final CustomerService customerService;
    private final ServiceCatalogService serviceCatalogService;
    private final ServiceOptionService serviceOptionService;
    private final AppointmentAlgorithmService appointmentAlgorithmService;
    private final NotificationService notificationService;

    public List<WaitingList> getActiveWaitingList() {
        return waitingListRepository.findByStatusOrderByCreatedAtAsc(WaitingListStatus.ACTIVE);
    }

    public List<WaitingList> getActiveByCustomer(Integer customerId) {
        return waitingListRepository.findByCustomerCustomerIdAndStatusOrderByCreatedAtDesc(customerId, WaitingListStatus.ACTIVE);
    }

    public WaitingList getById(Integer waitingListId) {
        return waitingListRepository.findById(waitingListId)
                .orElseThrow(() -> new ResourceNotFoundException("Waiting list record not found: " + waitingListId));
    }

    @Transactional
    public WaitingList create(WaitingList request) {
        glowbook.entity.Service service = serviceCatalogService.getActiveById(request.getService().getServiceId());
        ServiceOption option = serviceOptionService.getActiveByService(service.getServiceId(), request.getServiceOption().getOptionId());
        Customer customer = resolveCustomer(request);

        request.setCustomer(customer);
        request.setService(service);
        request.setServiceOption(option);
        request.setStatus(WaitingListStatus.ACTIVE);

        return waitingListRepository.save(request);
    }

    @Transactional
    public WaitingList cancel(Integer waitingListId) {
        WaitingList waitingList = getById(waitingListId);
        waitingList.setStatus(WaitingListStatus.CANCELLED);
        return waitingListRepository.save(waitingList);
    }

    @Transactional
    public void notifyMatchingCustomers(Integer serviceId, LocalDate appointmentDate) {
        List<WaitingList> candidates = waitingListRepository.findByServiceServiceIdAndPreferredDateAndStatusOrderByCreatedAtAsc(
                serviceId,
                appointmentDate,
                WaitingListStatus.ACTIVE
        );

        for (WaitingList waitingList : candidates) {
            boolean hasSlot = appointmentAlgorithmService.findAvailableSlots(serviceId, appointmentDate)
                    .stream()
                    .flatMap(slot -> slot.availableTimes().stream())
                    .anyMatch(time -> isInsidePreference(waitingList, time));

            if (hasSlot) {
                notificationService.createAndSendSms(
                        waitingList.getCustomer(),
                        null,
                        NotificationType.WAITING_LIST_MATCH,
                        "Uygun randevu bulundu",
                        "Talep ettiginiz hizmet icin uygun randevu saati acildi.",
                        waitingList.getPhone()
                );
                waitingList.setStatus(WaitingListStatus.NOTIFIED);
                waitingListRepository.save(waitingList);
            }
        }
    }

    @Transactional
    public WaitingList markConverted(Integer waitingListId) {
        WaitingList waitingList = getById(waitingListId);
        waitingList.setStatus(WaitingListStatus.CONVERTED);
        return waitingListRepository.save(waitingList);
    }

    private Customer resolveCustomer(WaitingList request) {
        if (request.getCustomer() == null || request.getCustomer().getCustomerId() == null) {
            requireGuestInfo(request);
            return null;
        }

        Customer customer = customerService.getById(request.getCustomer().getCustomerId());
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            request.setCustomerName(customer.getFirstName());
        }
        if (request.getCustomerSurname() == null || request.getCustomerSurname().isBlank()) {
            request.setCustomerSurname(customer.getLastName());
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            request.setPhone(customer.getPhone());
        }
        return customer;
    }

    private void requireGuestInfo(WaitingList request) {
        if (isBlank(request.getCustomerName()) || isBlank(request.getCustomerSurname()) || isBlank(request.getPhone())) {
            throw new BusinessException("Customer name, surname and phone are required for guest waiting list records");
        }
    }

    private boolean isInsidePreference(WaitingList waitingList, LocalTime time) {
        LocalTime start = waitingList.getPreferredStartTime();
        LocalTime end = waitingList.getPreferredEndTime();
        return (start == null || !time.isBefore(start)) && (end == null || !time.isAfter(end));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
