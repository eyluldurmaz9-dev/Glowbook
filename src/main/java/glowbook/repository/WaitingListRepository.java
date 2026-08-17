package glowbook.repository;

import glowbook.entity.WaitingList;
import glowbook.entity.WaitingListStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WaitingListRepository extends JpaRepository<WaitingList, Integer> {

    List<WaitingList> findByStatusOrderByCreatedAtAsc(WaitingListStatus status);

    List<WaitingList> findByCustomerCustomerIdAndStatusOrderByCreatedAtDesc(Integer customerId, WaitingListStatus status);

    List<WaitingList> findByServiceServiceIdAndPreferredDateAndStatusOrderByCreatedAtAsc(
            Integer serviceId,
            LocalDate preferredDate,
            WaitingListStatus status
    );

    /** Data-cleanup merge finders: re-point every waiting-list row off a duplicate catalog row before it is deleted. */
    List<WaitingList> findByServiceServiceId(Integer serviceId);

    List<WaitingList> findByServiceOptionOptionId(Integer optionId);
}
